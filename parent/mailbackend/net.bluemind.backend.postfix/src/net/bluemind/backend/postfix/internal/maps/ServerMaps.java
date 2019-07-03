/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.postfix.internal.maps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.backend.postfix.internal.maps.generators.IMapGenerator;
import net.bluemind.backend.postfix.internal.maps.generators.MasterRelayTransportMap;
import net.bluemind.backend.postfix.internal.maps.generators.TransportMap;
import net.bluemind.backend.postfix.internal.maps.generators.VirtualAliasMap;
import net.bluemind.backend.postfix.internal.maps.generators.VirtualDomainsMap;
import net.bluemind.backend.postfix.internal.maps.generators.VirtualMailboxesMap;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ServerMaps {
	private final ItemValue<Server> server;
	private final Map<String, DomainInfo> domainInfoByUid;
	private final Map<String, ItemValue<Server>> edgeNextHopByDomainUid;
	private final Collection<MapRow> mapRows;

	private final Collection<IMapGenerator> maps = new ArrayList<>();

	private ServerMaps(ItemValue<Server> server, Map<String, DomainInfo> domainInfoByUid,
			Map<String, ItemValue<Server>> edgeNextHopByDomainUid, List<MapRow> mapRows) {
		this.server = server;
		this.domainInfoByUid = domainInfoByUid;
		this.edgeNextHopByDomainUid = edgeNextHopByDomainUid;
		this.mapRows = mapRows;

		initMaps();
	}

	private ServerMaps(ItemValue<Server> server) {
		this.server = server;
		this.domainInfoByUid = Collections.emptyMap();
		this.edgeNextHopByDomainUid = Collections.emptyMap();
		this.mapRows = Collections.emptyList();

		initMaps();
	}

	private void initMaps() {
		maps.add(new VirtualDomainsMap(
				domainInfoByUid.values().stream().map(domainInfo -> domainInfo.domain).collect(Collectors.toList())));

		maps.add(VirtualMailboxesMap.init(domainInfoByUid, mapRows));
		maps.add(new VirtualAliasMap(mapRows));

		maps.add(new TransportMap(edgeNextHopByDomainUid, mapRows));
		maps.add(MasterRelayTransportMap.init(domainInfoByUid));
	}

	public ItemValue<Server> getServer() {
		return server;
	}

	public Map<String, DomainInfo> getDomainInfoByUid() {
		return domainInfoByUid;
	}

	public Map<String, ItemValue<Server>> getEdgeNextHopByDomainUid() {
		return edgeNextHopByDomainUid;
	}

	public Collection<MapRow> getMapRows() {
		return mapRows;
	}

	public static Optional<ServerMaps> init(ItemValue<Server> server) {
		return init(Collections.emptyList(), server, Collections.emptyMap(), Collections.emptyList());
	}

	public static Optional<ServerMaps> init(List<ItemValue<Server>> servers, ItemValue<Server> server,
			Map<String, DomainInfo> domainInfoByUid, List<MapRow> mapRows) {
		if (!server.value.tags.contains(SmtpRoles.EDGE.tag()) && !server.value.tags.contains(SmtpRoles.SMTP.tag())) {
			return Optional.empty();
		}

		IServer iserver = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		List<Assignment> serverAssignments = iserver.getServerAssignments(server.uid);

		Map<String, DomainInfo> assignedDomainInfoByUid = serverAssignments.stream()
				.filter(assignment -> (assignment.tag.equals(SmtpRoles.EDGE.tag())
						|| assignment.tag.equals(SmtpRoles.SMTP.tag()))
						&& domainInfoByUid.containsKey(assignment.domainUid))
				.collect(Collectors.toMap(a -> a.domainUid, a -> domainInfoByUid.get(a.domainUid),
						(domainInfo1, domainInfo2) -> domainInfo1));

		if (assignedDomainInfoByUid.isEmpty()) {
			return Optional.of(new ServerMaps(server));
		}

		List<Assignment> edgeAssignments = serverAssignments.stream()
				.filter(a -> (a.tag.equals(SmtpRoles.EDGE.tag()) && domainInfoByUid.containsKey(a.domainUid)))
				.collect(Collectors.toList());

		Map<String, ItemValue<Server>> edgeNextHopByDomainUid = new HashMap<>();
		for (Assignment edgeAssignment : edgeAssignments) {
			if (serverAssignments.stream()
					.filter(a -> (a.domainUid.equals(edgeAssignment.domainUid) && a.tag.equals(SmtpRoles.SMTP.tag())))
					.findFirst().isPresent()) {
				// Ignore EDGE tag if server is also tagged as SMTP for
				// the same domain
				continue;
			}

			List<String> edgeNextHopServersUids = iserver.byAssignment(edgeAssignment.domainUid, SmtpRoles.SMTP.tag());
			if (edgeNextHopServersUids.size() == 0) {
				throw new InvalidParameterException("Unable to find host tagued as " + SmtpRoles.SMTP.tag()
						+ " for domain uid: " + edgeAssignment.domainUid);
			}

			// FIXME: must use all SMTP server assigned to this domain
			// HA/Load balancing...
			Optional<ItemValue<Server>> smtpServer = servers.stream()
					.filter(s -> s.uid.equals(edgeNextHopServersUids.get(0))).findFirst();
			if (!smtpServer.isPresent()) {
				throw new InvalidParameterException("Unable to find host uid: " + edgeNextHopServersUids.get(0));
			}

			edgeNextHopByDomainUid.put(edgeAssignment.domainUid, smtpServer.get());
		}

		List<MapRow> mapRowsToManage = mapRows.stream()
				.filter(mapRow -> assignedDomainInfoByUid.containsKey(mapRow.domain.uid)).collect(Collectors.toList());

		return Optional.of(new ServerMaps(server, assignedDomainInfoByUid, edgeNextHopByDomainUid, mapRowsToManage));
	}

	public void writeFlatMaps() {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		for (IMapGenerator map : maps) {
			nodeClient.writeFile(map.getMapFileName() + "-flat",
					new ByteArrayInputStream(map.generateMap().getBytes()));
		}
	}

	public void enableMaps() {
		INodeClient nodeClient = NodeActivator.get(server.value.address());

		UpdateMapScript updateMapScript = new UpdateMapScript(maps);
		nodeClient.writeFile(UpdateMapScript.SCRIPT_FILENAME,
				new ByteArrayInputStream(updateMapScript.getContent().getBytes()));
		TaskRef tr = nodeClient.executeCommand("chmod +x " + UpdateMapScript.SCRIPT_FILENAME);
		NCUtils.waitFor(nodeClient, tr);

		tr = nodeClient.executeCommand(UpdateMapScript.SCRIPT_FILENAME);
		NCUtils.waitFor(nodeClient, tr);

		tr = nodeClient.executeCommand("rm -f " + UpdateMapScript.SCRIPT_FILENAME);
		NCUtils.waitFor(nodeClient, tr);
	}

	private class UpdateMapScript {
		public static final String SCRIPT_FILENAME = "/etc/postfix/updateMaps.sh";

		private Map<String, Object> templateData = new HashMap<>();

		public UpdateMapScript(Collection<IMapGenerator> mapsGenerator) {
			fillTemplateData(mapsGenerator);
		}

		private void fillTemplateData(Collection<IMapGenerator> mapsGenerator) {
			String[] fileNames = mapsGenerator.stream().map(mg -> mg.getMapFileName()).toArray(String[]::new);
			templateData.put("mapsfilenames", fileNames);
		}

		public String getContent() {
			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(getClass(), "/templates");
			Template template;
			try {
				template = cfg.getTemplate("updateMaps.sh");
			} catch (IOException e) {
				throw new ServerFault(e);
			}

			StringWriter sw = new StringWriter();
			try {
				template.process(templateData, sw);
			} catch (Exception e) {
				throw new ServerFault(e);
			}

			if (!sw.toString().endsWith(System.lineSeparator())) {
				sw.append(System.lineSeparator());
			}

			return sw.toString();
		}
	}

	public static enum SmtpRoles {
		SMTP("mail/smtp"), EDGE("mail/smtp-edge");

		private final String tag;

		SmtpRoles(String tag) {
			this.tag = tag;
		}

		public String tag() {
			return tag;
		}

		public static boolean contains(String tag) {
			for (SmtpRoles smtpRole : values()) {
				if (smtpRole.tag.equals(tag)) {
					return true;
				}
			}

			return false;
		}

		public static String[] tags() {
			String[] tags = new String[SmtpRoles.values().length];

			for (int i = 0; i < SmtpRoles.values().length; i++) {
				tags[i] = SmtpRoles.values()[i].tag;
			}

			return tags;
		}
	};
}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.nginx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.tag.api.TagDescriptor;

public class NginxService {
	private static Logger logger = LoggerFactory.getLogger(NginxService.class);

	private List<INginxConfigUpdater> searchUpdaters() throws ServerFault {
		List<INginxConfigUpdater> updaters = new ArrayList<>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.nginx.update");
		if (point == null) {
			throw new ServerFault("point net.bluemind.nginx.update not found");
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("updater")) {
					INginxConfigUpdater updater = null;
					try {
						updater = (INginxConfigUpdater) e.createExecutableExtension("impl");
					} catch (CoreException e1) {
						throw new ServerFault(e1);
					}
					updaters.add(updater);
				}
			}
		}

		return updaters;
	}

	/**
	 * Get all servers matching tag.
	 * 
	 * If no tags specified, return all server tags as "bm/nginx" or "bm/nginx-edge"
	 * 
	 * @param tags matching tag - support only "bm/nginx" or "bm/nginx-edge"
	 * @return
	 * @throws ServerFault
	 */
	private List<String> getTaggedServers(TagDescriptor... tags) throws ServerFault {
		Set<String> tagsAsString = new HashSet<>();
		if (tags.length != 0) {
			tagsAsString.addAll(Arrays.stream(tags)
					.filter(tag -> tag == TagDescriptor.bm_nginx || tag == TagDescriptor.bm_nginx_edge)
					.map(tag -> tag.getTag()).collect(Collectors.toList()));
		} else {
			tagsAsString.addAll(Arrays.asList(TagDescriptor.bm_nginx.getTag(), TagDescriptor.bm_nginx_edge.getTag()));
		}

		return Topology.getIfAvailable()
				.map(t -> t.nodes().stream()
						.filter(server -> server.value.tags.stream().anyMatch(tag -> tagsAsString.contains(tag)))
						.map(s -> s.value.address()).collect(Collectors.toList()))
				.orElse(Arrays.asList("127.0.0.1"));
	}

	private byte[] serverNameContent(String address) {
		return ("server_name " + address + ";\n").getBytes();
	}

	private byte[] externalUrlContent(String address) {
		return ("set $bmexternalurl " + address + ";\n").getBytes();
	}

	/**
	 * Reload NGinx & FPM on all servers tagged as "bm/nginx" and "bm/nginx-edge"
	 * 
	 * @throws ServerFault
	 */
	public void reloadHttpd() throws ServerFault {
		getTaggedServers().forEach(server -> reloadHttpd(NodeActivator.get(server)));
	}

	/**
	 * Reload NGinx & FPM on specific server
	 * 
	 * @param nc
	 * @throws ServerFault
	 */
	public void reloadHttpd(INodeClient nc) throws ServerFault {
		NCUtils.forget(nc, "service bm-php-fpm reload");
		NCUtils.forget(nc, "service bm-nginx reload");
	}

	public void updateFileHostingMaxSize(long fileHostingMaxSize) {
		doUpdate(fileHostingMaxSize, (updater, nc, size) -> updater.updateFilehostingSize(nc, size));
	}

	public void updateMessageSize(long messageSizeLimit) {
		doUpdate(messageSizeLimit, (updater, nc, size) -> updater.updateMessageSize(nc, size));
	}

	private interface NginxConfigUpdater {
		void run(INginxConfigUpdater updater, INodeClient nc, long size) throws ServerFault;
	}

	private void doUpdate(long fileHostingMaxSize, NginxConfigUpdater nginxUpdater) {
		List<String> taggedServers = getTaggedServers();

		List<INginxConfigUpdater> updaters = searchUpdaters();
		logger.info("Found {} nginx config updaters", updaters.size());

		logger.info("Distributing new settings to {} servers", taggedServers.size());
		for (String taggedServer : taggedServers) {
			logger.info("Distributing new settings to {}", taggedServer);
			INodeClient nc = NodeActivator.get(taggedServer);

			for (INginxConfigUpdater updater : updaters) {
				try {
					nginxUpdater.run(updater, nc, fileHostingMaxSize);
				} catch (ServerFault sf) {
					logger.warn("Cannot update nginx config on server {}:{}", taggedServer, sf.getMessage());
					throw sf;
				}
			}

			reloadHttpd(nc);
		}
	}

	public void updateSwPassword(String swPassword) {
		getTaggedServers().forEach(server -> {
			logger.info("update htpasswd on {}", server);
			INodeClient nc = NodeActivator.get(server);
			NCUtils.exec(nc, "/usr/bin/htpasswd -bc /etc/nginx/sw.htpasswd admin '" + swPassword + "'");
			reloadHttpd(nc);
		});
	}

	public void updateWorkerConnection(String workerConnections) {
		StringWriter sw = new StringWriter();
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("worker_connections", workerConnections);
			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(getClass(), "/templates");
			Template template = cfg.getTemplate("events.ftl");
			template.process(data, sw);
		} catch (TemplateException | IOException e) {
			throw new ServerFault(e);
		}

		getTaggedServers().forEach(server -> {
			logger.info("update worker_connections on {}", server);
			INodeClient nc = NodeActivator.get(server);
			nc.writeFile("/etc/nginx/global.d/events.conf", new ByteArrayInputStream(sw.toString().getBytes()));
			reloadHttpd(nc);
		});
	}

	public void updateExternalUrl(String externalUrl) {
		getTaggedServers().forEach(server -> updateExternalUrl(server, externalUrl));
	}

	public void updateExternalUrl(String nginxServerIp, String externalUrl) {
		logger.info("update bm-servername.conf & bm-externalurl.conf on {}", nginxServerIp);
		INodeClient nc = NodeActivator.get(nginxServerIp);
		nc.writeFile("/etc/nginx/bm-servername.conf", new ByteArrayInputStream(serverNameContent(externalUrl)));
		nc.writeFile("/etc/nginx/bm-externalurl.conf", new ByteArrayInputStream(externalUrlContent(externalUrl)));
		reloadHttpd(nc);
	}

	public void updateTickUpstream(String tickIp) {
		getTaggedServers(TagDescriptor.bm_nginx).forEach(server -> updateTickUpstream(server, tickIp));
	}

	public void updateTickUpstream(String nginxServerIp, String tickIp) {
		logger.info("Update bm-upstream-tick.conf on {}", nginxServerIp);
		INodeClient nc = NodeActivator.get(nginxServerIp);
		nc.writeFile("/etc/bm-tick/bm-upstream-tick.conf",
				new ByteArrayInputStream(String.format("server %s:8888;", tickIp).getBytes()));
		reloadHttpd(nc);
	}
}

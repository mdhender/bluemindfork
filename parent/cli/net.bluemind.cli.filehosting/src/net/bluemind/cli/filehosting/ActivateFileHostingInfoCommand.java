/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.filehosting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "activate", description = "Activate Filehosting")
public class ActivateFileHostingInfoCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Option(names = "--domain", description = "domain name, filehosting is actived on all domains if not specified")
	public String domain;

	@Option(names = "--server-uid", description = "Sets the responsible server. Checks for an existing server or uses the core server if not specified")
	public String server;

	@Option(names = "--group", description = "Adds the necessary role to this group if the group exists")
	public String group;

	@Override
	public void run() {
		ctx.info("Activating Filehosting...");

		String server = tagServer();
		assignDomain(server);
		addRole();
	}

	private String tagServer() {
		IServer serverService = ctx.adminApi().instance(IServer.class, "default");

		if (server != null) {
			ItemValue<Server> destServer = serverService.getComplete(server);
			if (!destServer.value.tags.contains(TagDescriptor.bm_filehosting.getTag())) {
				ctx.info("Assigning tag filehosting to server " + server);
				List<String> tags = new ArrayList<>(destServer.value.tags);
				tags.add(TagDescriptor.bm_filehosting.getTag());
				serverService.setTags(server, tags);
			} else {
				ctx.info("Server {} is already filehosting ready", server);
			}
			return server;
		} else {
			List<ItemValue<Server>> allServers = serverService.allComplete();
			List<ItemValue<Server>> tagged = allServers.stream()
					.filter(s -> s.value.tags.contains(TagDescriptor.bm_filehosting.getTag()))
					.collect(Collectors.toList());
			if (!tagged.isEmpty()) {
				String firstTaggedServer = tagged.stream().map(s -> s.uid).findFirst().get();
				ctx.info("Server {} is already filehosting ready, using it for assignment", firstTaggedServer);
				return firstTaggedServer;
			} else {
				for (ItemValue<Server> untaggedServer : allServers) {
					if (untaggedServer.value.tags.contains(TagDescriptor.bm_core.getTag())) {
						List<String> tags = new ArrayList<>(untaggedServer.value.tags);
						tags.add(TagDescriptor.bm_filehosting.getTag());
						serverService.setTags(untaggedServer.uid, tags);
						ctx.info("Server {} has been chosen for assignment", untaggedServer);
						return untaggedServer.uid;
					}
				}
			}
		}
		throw new CliException("No server has been found for assignment");
	}

	private void assignDomain(String server) {

		List<String> domains = getDomains();

		IServer serverService = ctx.adminApi().instance(IServer.class, "default");
		for (String domainUid : domains) {
			if (serverService.byAssignment(domainUid, TagDescriptor.bm_filehosting.getTag()).isEmpty()) {
				ctx.info("Activating filehosting for domain " + domainUid);
				serverService.assign(server, domainUid, TagDescriptor.bm_filehosting.getTag());
			} else {
				ctx.info("Filehosting is already active in domain " + domainUid);
			}
		}
	}

	private void addRole() {
		if (group == null) {
			return;

		}
		List<String> domains = getDomains();
		for (String domainUid : domains) {
			IGroup groupService = ctx.adminApi().instance(IGroup.class, domainUid);
			ItemValue<Group> resolvedGroup = groupService.byName(group);
			if (resolvedGroup != null) {
				ctx.info("Adding roles to group " + group + " for domain " + domain);
				Set<String> roles = new HashSet<>(groupService.getRoles(resolvedGroup.uid));
				roles.add("canRemoteAttach");
				roles.add("canUseFilehosting");
				groupService.setRoles(resolvedGroup.uid, roles);
			} else {
				ctx.info("Group " + group + "does not exist in domain " + domain);
			}
		}

	}

	private List<String> getDomains() {
		List<String> domains = new ArrayList<>();
		if (domain != null) {
			domains.add(new CliUtils(ctx).getDomainUidFromDomain(domain));
		} else {
			IDomains domainService = ctx.adminApi().instance(IDomains.class);
			domains.addAll(domainService.all().stream().filter(d -> !d.uid.endsWith("global.virt")).map(d -> d.uid)
					.collect(Collectors.toList()));
		}
		return domains;
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("filehosting");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ActivateFileHostingInfoCommand.class;
		}

	}

}

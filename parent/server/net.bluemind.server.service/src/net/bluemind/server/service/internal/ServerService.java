/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

package net.bluemind.server.service.internal;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.IServerHook;

public final class ServerService implements IServer {

	private static final Logger logger = LoggerFactory.getLogger(ServerService.class);
	private final ServerStoreService storeService;
	private final List<IServerHook> serverHooks;
	private final ServerValidator validator;
	private BmContext bmContext;
	private RBACManager rbacManager;
	private Sanitizer sanitizer;

	/**
	 * @param pool
	 * @param installation
	 * @param securityContext
	 * @param nodefactory
	 * @param serverhooks
	 * @throws ServerFault
	 */
	public ServerService(BmContext context, Container installation, List<IServerHook> serverhooks) throws ServerFault {
		this.serverHooks = serverhooks;
		this.bmContext = context;
		this.validator = new ServerValidator();
		storeService = new ServerStoreService(bmContext, installation);

		rbacManager = new RBACManager(context);
		sanitizer = new Sanitizer(context);

	}

	@Override
	public TaskRef create(String uid, Server srv) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);

		if (!uid.toLowerCase().equals(uid)) {
			throw new ServerFault("Only lowercase is allowed for server uid");
		}

		if (uid.contains("_")) {
			throw new ServerFault("server uid does not support '_'");
		}

		sanitizer.create(srv);
		validator.validate(srv);

		for (IServerHook hook : serverHooks) {
			hook.beforeCreate(bmContext, uid, srv);
		}

		logger.info("create server {} : {} tags {}", uid, srv, srv.tags);
		storeService.create(uid, getSummary(srv), srv);

		return bmContext.provider().instance(ITasksManager.class).run((m) -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(serverHooks.size() + srv.tags.size(), "create server");

			ItemValue<Server> iv = ItemValue.create(Item.create(uid, null), srv);
			for (IServerHook hook : serverHooks) {
				logger.info("{}", hook);
				hook.onServerCreated(bmContext, iv);
				monitor.progress(1, null);
			}

			logger.info("Processing {} tags for {}", srv.tags.size(), srv.address());
			for (String tag : srv.tags) {
				onTag(uid, iv, tag);
				monitor.progress(1, "tag " + tag);
			}
		}));
	}

	private String getSummary(Server srv) {
		return srv.fqdn != null ? srv.fqdn : srv.ip;
	}

	@Override
	public TaskRef update(String uid, Server srv) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);

		// FIXME check tags validity (ex : smtp-edg/smtp are incompatibles)

		ItemValue<Server> previous = getComplete(uid);
		if (previous == null) {
			throw new ServerFault("Server " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		sanitizer.update(previous.value, srv);
		validator.validate(srv);

		Set<String> newTags = Sets.newHashSet(srv.tags);
		Set<String> prevTags = Sets.newHashSet(previous.value.tags);
		Set<String> tagged = Sets.difference(newTags, prevTags);
		Set<String> untagged = Sets.difference(prevTags, newTags);

		List<Assignment> asses = storeService.getServerAssignements(uid);
		List<Assignment> untaggedError = new ArrayList<>(untagged.size());
		for (Assignment ass : asses) {
			if (untagged.contains(ass.tag)) {
				untaggedError.add(ass);
			}
		}

		if (!untaggedError.isEmpty()) {
			StringBuilder w = new StringBuilder();
			for (Assignment e : untaggedError) {
				w.append(" (" + e.domainUid + "," + e.tag + ")");
			}
			throw new ServerFault("server is tagged for " + w, ErrorCode.INVALID_PARAMETER);
		}

		for (IServerHook hook : serverHooks) {
			hook.beforeUpdate(bmContext, uid, srv, previous.value);
		}

		storeService.update(uid, getSummary(srv), srv);
		logger.debug("Should call hook: {}", previous);
		// FIXME tag on update ?

		return bmContext.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(serverHooks.size() + tagged.size() + untagged.size(), "update server");

			for (String tag : tagged) {
				onTag(uid, previous, tag);
			}
			for (String tag : untagged) {
				onUntagged(uid, previous, tag);
			}
			for (IServerHook hook : serverHooks) {
				hook.onServerUpdated(bmContext, previous, srv);
			}

		}));
	}

	@Override
	public ItemValue<Server> getComplete(String uid) throws ServerFault {
		rbacManager ///
				.forDomain(bmContext.getSecurityContext().getContainerUid())
				// because domain admincan "read" server"
				.check(BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGER, BasicRoles.ROLE_MANAGE_SERVER);
		ItemValue<Server> ret = storeService.get(uid, null);
		return ret;
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		ItemValue<Server> previous = getComplete(uid);

		if (previous == null) {
			throw new ServerFault("Server " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		List<Assignment> asses = storeService.getServerAssignements(uid);
		if (!asses.isEmpty()) {
			StringBuilder w = new StringBuilder();
			for (Assignment e : asses) {
				w.append(" (" + e.domainUid + "," + e.tag + ")");
			}
			throw new ServerFault("Server " + uid + " is assigned to domain, unassign before delete");
		}
		storeService.delete(uid);
		logger.debug("Should call hook: {}", previous);
		for (String tag : previous.value.tags) {
			onUntagged(uid, previous, tag);
		}

		for (IServerHook hook : serverHooks) {
			hook.onServerDeleted(bmContext, previous);
		}

	}

	@Override
	public List<ItemValue<Server>> allComplete() throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGER,
				BasicRoles.ROLE_MANAGE_SERVER);
		return storeService.all();
	}

	@Override
	public String submit(String uid, String command) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		INodeClient client = client(uid);
		TaskRef ref = client.executeCommand(command);
		return ref.id;
	}

	private INodeClient client(String uid) throws ServerFault {
		ItemValue<Server> item = storeService.get(uid, null);

		if (item == null) {
			throw new ServerFault("Server " + uid + " not found");
		}

		return NodeActivator.get(item.value.address());
	}

	@Override
	public CommandStatus getStatus(String uid, String commandRef) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		INodeClient client = client(uid);
		TaskStatus status = client.getExecutionStatus(TaskRef.create(commandRef));
		CommandStatus cs = new CommandStatus();
		cs.complete = status.state.ended;
		cs.successful = status.state.succeed;
		cs.output = Arrays.asList(status.lastLogEntry);
		return cs;
	}

	@Override
	public CommandStatus submitAndWait(String uid, String command) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		INodeClient client = client(uid);

		TaskRef ref = client.executeCommand(command);
		ExitList e = NCUtils.waitFor(client, ref);
		CommandStatus cs = new CommandStatus();
		cs.complete = true;
		cs.successful = e.getExitCode() == 0;
		cs.output = new ArrayList<String>(e);
		return cs;
	}

	@Override
	public byte[] readFile(String uid, String path) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		return client(uid).read(path);
	}

	@Override
	public void writeFile(String uid, String path, byte[] content) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);
		client(uid).writeFile(path, new ByteArrayInputStream(content));
	}

	@Override
	public void assign(String serverUid, String domainUid, String tag) throws ServerFault {
		rbacManager.forDomain(domainUid).check(BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGE_SERVER);
		ItemValue<Server> server = storeService.get(serverUid, null);
		if (server == null) {
			throw new ServerFault(
					"Server with uid " + serverUid + " not found for assignment of " + tag + " to " + domainUid,
					ErrorCode.NOT_FOUND);
		}

		ItemValue<Domain> domain = bmContext.provider().instance(IDomains.class).get(domainUid);
		if (domain == null) {
			throw new ServerFault(
					"Domain with uid " + serverUid + " not found for assignment of " + tag + " to server " + serverUid,
					ErrorCode.NOT_FOUND);
		}

		if (!Sets.newHashSet(server.value.tags).contains(tag)) {
			throw new ServerFault("Assignement of server without " + tag + " tag refused.");
		}

		boolean alreadyAssigned = false;
		for (Assignment ass : storeService.getAssignments(domainUid)) {
			if (ass.serverUid.equals(serverUid) && ass.tag.equals(tag)) {
				alreadyAssigned = true;
				break;
			}
		}

		if (alreadyAssigned) {
			logger.info("Assigned {} as {} to {} (already assigned)", serverUid, tag, domainUid);
			return;
		}
		storeService.assign(serverUid, domainUid, tag);

		onAssigned(server, domain, tag);
		logger.info("Assigned {} as {} to {}", serverUid, tag, domainUid);

	}

	@Override
	public void unassign(String serverUid, String domainUid, String tag) throws ServerFault {
		rbacManager.forDomain(domainUid).check(BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGE_SERVER);
		ItemValue<Server> server = storeService.get(serverUid, null);
		if (server == null) {
			throw new ServerFault(
					"Server with uid " + serverUid + " not found for unassignment of " + tag + " to " + domainUid,
					ErrorCode.NOT_FOUND);
		}

		ItemValue<Domain> domain = bmContext.provider().instance(IDomains.class).get(domainUid);
		if (domain == null) {
			throw new ServerFault(
					"Domain with uid " + serverUid + " not found for assignment of " + tag + " to server " + serverUid,
					ErrorCode.NOT_FOUND);
		}

		onPreUnassigned(serverUid, server, domain, tag);

		storeService.unassign(serverUid, domainUid, tag);

		onUnassigned(serverUid, server, domain, tag);
		logger.info("Unassigned {} as {} from {}", serverUid, tag, domainUid);
	}

	private void onPreUnassigned(String serverUid, ItemValue<Server> server, ItemValue<Domain> domain, String tag) {
		logger.info("Server {}:{} pre-unassigned {} tag {}", serverUid, server.value.address(), domain.uid, tag);
		for (IServerHook hook : serverHooks) {
			hook.onServerPreUnassigned(bmContext, server, domain, tag);
		}
	}

	@Override
	public List<Assignment> getAssignments(String domainUid) throws ServerFault {
		rbacManager.forDomain(domainUid).check(Verb.Read.name(), BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGER,
				BasicRoles.ROLE_MANAGE_SERVER);
		return storeService.getAssignments(domainUid);
	}

	private void onTag(String uid, ItemValue<Server> iv, String tag) {
		logger.info("Server {}:{} tagged {}", uid, iv.value.address(), tag);
		for (IServerHook hook : serverHooks) {
			hook.onServerTagged(bmContext, iv, tag);
		}
	}

	private void onUntagged(String uid, ItemValue<Server> iv, String tag) {
		logger.info("Server {}:{} untagged {}", uid, iv.value.address(), tag);
		for (IServerHook hook : serverHooks) {
			try {
				hook.onServerUntagged(bmContext, iv, tag);
			} catch (Exception e) {
				logger.warn("error on server {} untag {}", uid, tag, e);
			}
		}
	}

	private void onAssigned(ItemValue<Server> server, ItemValue<Domain> domain, String tag) {
		logger.info("Server {}:{} assigned {} tag {}", server.uid, server.value.address(), domain.uid, tag);
		for (IServerHook hook : serverHooks) {
			hook.onServerAssigned(bmContext, server, domain, tag);
		}
	}

	private void onUnassigned(String serverUid, ItemValue<Server> server, ItemValue<Domain> domain, String tag) {
		logger.info("Server {}:{} unassigned {} tag {}", serverUid, server.value.address(), domain.uid, tag);
		for (IServerHook hook : serverHooks) {
			try {
				hook.onServerUnassigned(bmContext, server, domain, tag);
			} catch (Exception e) {
				logger.warn("error on server {} unassigned {} tag {}", serverUid, domain.uid, tag, e);
			}
		}
	}

	@Override
	public List<String> byAssignment(String domainUid, String tag) throws ServerFault {
		List<String> serverUids = new ArrayList<>();
		List<Assignment> assignments = getAssignments(domainUid);

		for (Assignment assignment : assignments) {
			if (assignment.tag.equals(tag)) {
				serverUids.add(assignment.serverUid);
			}
		}

		return serverUids;
	}

	@Override
	public TaskRef setTags(String uid, List<String> tags) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_SERVER);

		ItemValue<Server> previous = storeService.get(uid, null);

		if (previous == null) {
			throw new ServerFault("Server " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		Server updated = previous.value.copy();
		updated.tags = new ArrayList<>(tags);
		Set<String> newTags = Sets.newHashSet(updated.tags);
		Set<String> prevTags = Sets.newHashSet(previous.value.tags);
		Set<String> tagged = Sets.difference(newTags, prevTags);
		Set<String> untagged = Sets.difference(prevTags, newTags);

		List<Assignment> asses = storeService.getServerAssignements(uid);
		List<Assignment> untaggedError = new ArrayList<>(untagged.size());
		for (Assignment ass : asses) {
			if (untagged.contains(ass.tag)) {
				untaggedError.add(ass);
			}
		}

		previous.value.tags = new ArrayList<>(tags);
		if (!untaggedError.isEmpty()) {
			StringBuilder w = new StringBuilder();
			for (Assignment e : untaggedError) {
				w.append(" (" + e.domainUid + "," + e.tag + ")");
			}
			throw new ServerFault("server is tagged for " + w, ErrorCode.INVALID_PARAMETER);
		}
		storeService.update(uid, getSummary(updated), updated);

		return bmContext.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(tagged.size() + untagged.size(), "begin updating tags");

			for (String tag : tagged) {
				onTag(uid, previous, tag);
			}
			for (String tag : untagged) {
				onUntagged(uid, previous, tag);
			}
			for (IServerHook hook : serverHooks) {
				hook.onServerUpdated(bmContext, previous, updated);
			}
		}));
	}

	@Override
	public List<Assignment> getServerAssignments(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_ADMIN, BasicRoles.ROLE_MANAGER,
				BasicRoles.ROLE_MANAGE_SERVER);
		return storeService.getServerAssignements(uid);
	}

}

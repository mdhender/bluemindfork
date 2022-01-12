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
package net.bluemind.backend.cyrus.annotationdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.ConversationElement;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.FORMAT;
import net.bluemind.backend.cyrus.index.CyrusIndex;
import net.bluemind.backend.cyrus.index.UnknownVersion;
import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.MailboxDescriptor;
import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.Conversation.MessageRef;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.replica.api.IInternalMailConversation;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecDescriptor;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class ConversationSync {

	private static final Logger logger = LoggerFactory.getLogger(ConversationSync.class);
	private final BmContext context;
	private final String task;
	private final Create create;
	private final Update update;

	public ConversationSync(BmContext context, String task) {
		this.context = context;
		this.task = task;
		this.create = (conversationId, service, conversation) -> service.create(Long.toHexString(conversationId),
				conversation);
		this.update = (service, conversation) -> service.update(conversation.uid, conversation.value);
	}

	public ConversationSync(BmContext context, String task, Create create, Update update) {
		this.context = context;
		this.task = task;
		this.create = create;
		this.update = update;
	}

	@SuppressWarnings("serial")
	public class CyrusConversationDbInitException extends Exception {
		public CyrusConversationDbInitException(String task, Exception e) {
			super("Failed operation on " + task, e);
		}
	}

	public void execute(String domainUid, String userUid, IServerTaskMonitor monitor, DiagnosticReport report)
			throws CyrusConversationDbInitException {
		logger.info("migrating conversations of {}@{}", userUid, domainUid);
		monitor.log("migrating conversations of " + userUid + "@" + domainUid);

		ItemValue<Mailbox> box = context.provider().instance(IMailboxes.class, domainUid).getComplete(userUid);
		IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
		String dataLocation = getDataLocation(dir, box);
		ItemValue<Server> server = getImapServer(dataLocation);

		try {
			initConversationDb(server, domainUid, box, userUid);
		} catch (CyrusConversationDbInitException e1) {
			logger.warn("cannot init db", e1);
			monitor.log("cannot init db: " + e1.getMessage());
			throw e1;
		}
		try {
			syncConversationInfo(domainUid, server, box);
			updateUserSettings(domainUid, userUid);
		} catch (CyrusConversationDbInitException ex) {
			throw ex;
		} catch (Exception e) {
			logger.warn("Cannot create conversations", e);
			monitor.log("Cannot create conversations: " + e.getMessage());
			throw new CyrusConversationDbInitException(dataLocation, e);
		}
	}

	private void updateUserSettings(String domainUid, String userUid) {
		IUserSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, domainUid);
		settings.setOne(userUid, "mail_thread", "false");
	}

	private ItemValue<Server> getImapServer(String datalocation) throws CyrusConversationDbInitException {
		IServer serverApi = context.provider().instance(IServer.class, "default");
		try {
			return serverApi.getComplete(datalocation);
		} catch (ServerFault e) {
			throw new CyrusConversationDbInitException(task, e);
		}
	}

	private void initConversationDb(ItemValue<Server> server, String domainUid, ItemValue<Mailbox> box, String userUid)
			throws CyrusConversationDbInitException {
		try {
			INodeClient nc = NodeActivator.get(server.value.address());
			checkCyrusIndexVersion(nc, server, domainUid, box);
			String boxName = box.value.name + "@" + domainUid;
			NCUtils.execNoOut(nc, "/usr/sbin/ctl_conversationsdb -b " + boxName);
		} catch (Exception e) {
			throw new CyrusConversationDbInitException(task, e);
		}
	}

	private void checkCyrusIndexVersion(INodeClient nc, ItemValue<Server> server, String domainUid,
			ItemValue<Mailbox> box) throws Exception {
		String boxName = box.value.name + "@" + domainUid;

		if (needsReconstruct(nc, domainUid, box)) {
			logger.info("Mailbox {} needs reconstruction", boxName);
			NCUtils.execNoOut(nc, "reconstruct -r -V max user/" + boxName);
			logger.info("Rechecking Mailbox {}", boxName);
			if (needsReconstruct(nc, domainUid, box)) {
				throw new Exception("Reconstruct of " + boxName + " had no effect");
			}
			logger.info("Mailbox {} is ok now", boxName);
		}
	}

	private boolean needsReconstruct(INodeClient nc, String domainUid, ItemValue<Mailbox> box) throws IOException {
		String boxName = box.value.name + "@" + domainUid;
		String partition = domainUid.replace('.', '_');
		String mboxForApi = box.value.type.nsPrefix + box.value.name.replace('.', '^');
		List<String> folders = null;
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domainUid).getComplete(box.uid);
		try (Sudo su = Sudo.forUser(user, domainUid)) {
			IMailboxFolders folderService = ServerSideServiceProvider.getProvider(su.context)
					.instance(IMailboxFolders.class, partition, mboxForApi);
			folders = folderService.all() //
					.stream().filter(f -> !f.flags.contains(ItemFlag.Deleted)) //
					.map(f -> f.value.fullName) //
					.collect(Collectors.toList());
		}
		if (folders.isEmpty()) {
			logger.warn("No folders found for mailbox {}", boxName);
			return true;
		}

		boolean needsReconstruct = false;
		for (String folder : folders) {
			needsReconstruct |= checkIndexVersion(nc, domainUid, box.uid, boxName, folder);
		}
		return needsReconstruct;
	}

	private boolean checkIndexVersion(INodeClient nc, String domainUid, String userUid, String mailbox, String folder)
			throws IOException {
		CyrusContext cyrusContext = CyrusContext.build(context, domainUid, userUid, folder);
		File indexFileName = new File(cyrusContext.index);
		File parent = indexFileName.getParentFile();
		boolean exists = nc.listFiles(parent.getAbsolutePath()).stream()
				.anyMatch(f -> f.getName().equals(indexFileName.getName()));
		if (exists) {
			InputStream fis = null; // does not support autocloseable
			try {
				fis = nc.openStream(cyrusContext.index);
				CyrusIndex index = new CyrusIndex(fis);
				index.readHeader();
				logger.info("Cyrus index version of {}-{}: {}", mailbox, folder, index.getHeader().version);
				return index.getHeader().version < 13;
			} catch (UnknownVersion v) {
				logger.info("Cyrus index of {}-{} needs reconstruction: {}", mailbox, folder, v.getMessage());
				return true;
			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		} else {
			logger.warn("Cannot find index file of folder {}/{} --> {}", mailbox, folder, cyrusContext.index);
			return true;
		}
	}

	private void syncConversationInfo(String domainUid, ItemValue<Server> server, ItemValue<Mailbox> box)
			throws Exception {
		try {
			handleMbox(domainUid, server, box);
		} catch (Exception e) {
			logger.warn("Cannot handle mbox {}", box, e);
			throw e;
		}
	}

	private void handleMbox(String domainUid, ItemValue<Server> server, ItemValue<Mailbox> box) throws Exception {
		logger.info("Creating conversations of mailbox {} on server {}", box.value.name, server.uid);
		AnnotationDb parser = new AnnotationDb();
		INodeClient nodeClient = NodeActivator.get(server.value.address());
		NodeExec parseCyrusAnnotationDb = parseConversationDbContent(domainUid, box, parser, nodeClient);
		try {
			parseCyrusAnnotationDb.ret.get(1, TimeUnit.HOURS);
		} catch (TimeoutException to) {
			logger.info("Timeout while creating annotation db of mailbox {}", box.value.name);
			nodeClient.interrupt(ExecDescriptor.forTask(parseCyrusAnnotationDb.taskRef));
			throw to;
		}
		ConversationInfo conversationInfos = parser.get();
		logger.info("Migrating {} conversations of mailbox {} on server {}", conversationInfos.conversations.size(),
				box.value.name, server.uid);
		if (!conversationInfos.conversations.isEmpty()) {
			MailboxReplicaRootDescriptor descriptor = MailboxReplicaRootDescriptor.create(box.value);
			CyrusPartition cyrusPartition = CyrusPartition.forServerAndDomain(box.value.dataLocation, domainUid);
			IReplicatedMailboxesRootMgmt subtreeMgmt = context.provider().instance(IReplicatedMailboxesRootMgmt.class,
					cyrusPartition.name);
			subtreeMgmt.create(descriptor);
			migrateConversations(domainUid, server, box, conversationInfos);
		}
	}

	private void migrateConversations(String domainUid, ItemValue<Server> server, ItemValue<Mailbox> box,
			ConversationInfo conversationInfos) throws Exception {

		Map<Long, String> containers = getContainerIdByOwner(box.uid, server.uid);
		Map<String, MessageRef> bodyMessageMapping = getBodyMessageMapping(box.uid, conversationInfos, containers,
				server.uid);

		conversationInfos.conversations.forEach(conversation -> {
			IInternalMailConversation service = getService(domainUid, box.uid);
			ItemValue<Conversation> existingConversation = service
					.getComplete(Long.toHexString(conversation.conversationId));
			if (existingConversation == null) {
				Conversation newConversation = new Conversation();
				newConversation.messageRefs = new ArrayList<>();
				addMessages(conversation, newConversation, box.uid, context.getMailboxDataSource(server.uid),
						containers, bodyMessageMapping);
				if (!newConversation.messageRefs.isEmpty()) {
					create.apply(conversation.conversationId, service, newConversation);
				}
			} else {
				int currentMsgCount = existingConversation.value.messageRefs.size();
				addMessages(conversation, existingConversation.value, box.uid, context.getMailboxDataSource(server.uid),
						containers, bodyMessageMapping);
				if (existingConversation.value.messageRefs.size() > currentMsgCount) {
					update.apply(service, existingConversation);
				}
			}
		});
	}

	private Map<String, MessageRef> getBodyMessageMapping(String mbox, ConversationInfo conversationInfos,
			Map<Long, String> containers, String dataLocation) throws Exception {

		List<String> messageIds = conversationInfos.conversations.stream() //
				.filter(c -> c.format == FORMAT.MESSAGE_ID) //
				.flatMap(conversation -> Stream.of(conversation.uids.toArray(new String[0]))) //
				.map(id -> id.toLowerCase()).collect(Collectors.toList());
		return messageIdsToMapping(mbox, messageIds, containers, dataLocation);
	}

	private Map<String, MessageRef> messageIdsToMapping(String mbox, List<String> messageIds,
			Map<Long, String> containers, String dataLocation) throws Exception {
		String cmd = "select ci.id, mr.internal_date, mr.container_id, mb.message_id from t_message_body mb"
				+ " join t_mailbox_record mr on mr.message_body_guid = mb.guid"
				+ " join t_container_item ci on mr.item_id = ci.id"
				+ " where mr.container_id = any (?) and lower(mb.message_id) = any(?)";

		Map<String, MessageRef> mapping = new HashMap<>();
		DataSource pool = context.getMailboxDataSource(dataLocation);
		try (Connection con = pool.getConnection(); PreparedStatement stmt = con.prepareStatement(cmd)) {
			String[] msgs = messageIds.toArray(new String[0]);
			Array msg = con.createArrayOf("varchar", msgs);
			Long[] conts = containers.keySet().toArray(new Long[0]);
			Array cont = con.createArrayOf("bigint", conts);

			stmt.setArray(1, cont);
			stmt.setArray(2, msg);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					long itemId = rs.getLong(1);
					Date date = rs.getTimestamp(2);
					long containerId = rs.getLong(3);
					String messageId = rs.getString(4).toLowerCase();

					MessageRef message = new MessageRef();
					message.folderUid = IMailReplicaUids.uniqueId(containers.get(containerId));
					message.itemId = itemId;
					message.date = date;
					mapping.put(messageId, message);

				}
			}
		}

		return mapping;
	}

	private Map<Long, String> getContainerIdByOwner(String mbox, String dataLocation) throws SQLException {
		DataSource pool = context.getMailboxDataSource(dataLocation);

		String cmd = "SELECT id, uid from t_container where owner = ? and container_type = 'mailbox_records'";
		Map<Long, String> mapping = new HashMap<>();
		try (Connection con = pool.getConnection(); PreparedStatement stmt = con.prepareStatement(cmd)) {
			stmt.setString(1, mbox);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					long itemId = rs.getLong(1);
					String uid = rs.getString(2);
					mapping.put(itemId, uid);

				}
			}
		}

		return mapping;
	}

	private void addMessages(ConversationElement conversation, Conversation dbConversation, String owner,
			DataSource pool, Map<Long, String> containers, Map<String, MessageRef> bodyMessageMapping) {
		conversation.uids.forEach(uid -> {
			try {
				if (conversation.format == FORMAT.MESSAGE_ID) {
					MessageRef messageContext = bodyMessageMapping.get(uid.toLowerCase());
					if (messageContext != null && !dbConversation.messageRefs.contains(messageContext)) {
						dbConversation.messageRefs.add(messageContext);
					} else {
						logger.info("no body found for uid {}", uid.toLowerCase());
					}
				} else {
					getRecordUidByBodyRef(uid, owner, pool, containers).ifPresent(msg -> {
						if (!dbConversation.messageRefs.contains(msg)) {
							dbConversation.messageRefs.add(msg);
						}
					});
				}
			} catch (Exception e) {
				logger.warn("Cannot handle body ref {} of owner {}", uid, owner, e);
			}
		});
	}

	private Optional<MessageRef> getRecordUidByBodyRef(String bodyRef, String owner, DataSource pool,
			Map<Long, String> containers) throws Exception {
		String cmd = "select ci.id, re.internal_date, re.container_id from t_mailbox_record re "//
				+ "join t_container_item ci on re.item_id = ci.id " //
				+ "where re.message_body_guid = decode(?, 'hex') and re.container_id = any(?)";

		try (Connection con = pool.getConnection(); PreparedStatement stmt = con.prepareStatement(cmd)) {
			stmt.setString(1, bodyRef);
			Long[] conts = containers.keySet().toArray(new Long[0]);
			Array cont = con.createArrayOf("bigint", conts);
			stmt.setArray(2, cont);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					String containerList = String.join(",",
							containers.keySet().stream().map(String::valueOf).collect(Collectors.toList()));
					logger.info("Cannot find record referencing body guid {} of owner {} in containers {}", bodyRef,
							owner, containerList);
					return Optional.empty();
				}
				MessageRef messageId = new MessageRef();
				messageId.folderUid = IMailReplicaUids.uniqueId(containers.get(rs.getLong(3)));
				messageId.itemId = rs.getLong(1);
				messageId.date = rs.getTimestamp(2);
				return Optional.of(messageId);
			}
		}

	}

	private IInternalMailConversation getService(String domain, String userUid) {
		String uid = IMailReplicaUids.conversationSubtreeUid(domain, userUid);
		return context.provider().instance(IInternalMailConversation.class, uid);
	}

	private static String getDataLocation(IDirectory dir, ItemValue<Mailbox> box) {
		String dataLocation = dir.findByEntryUid(box.uid).dataLocation;
		if (null == dataLocation) {
			dataLocation = Topology.get().core().uid;
		}
		return dataLocation;
	}

	private NodeExec parseConversationDbContent(String domainUid, ItemValue<Mailbox> box, AnnotationDb parser,
			INodeClient nodeClient) {

		CompletableFuture<Void> ret = new CompletableFuture<>();
		NodeExec exec = new NodeExec(ret);
		try {
			String command = "/usr/sbin/ctl_conversationsdb -d " + box.value.name + "@" + domainUid;

			ExecRequest cmd = ExecRequest.anonymous(command);
			nodeClient.asyncExecute(cmd, new ProcessHandler() {

				@Override
				public void log(String out, boolean isContinued) {
					parser.accept(out);
				}

				@Override
				public void completed(int exitCode) {
					ret.complete(null);
				}

				@Override
				public void starting(String taskRef) {
					exec.setTaskRef(taskRef);
				}

			});

		} catch (Exception e) {
			logger.warn("Cannot read conversation db content of box {}", box);
			throw e;
		}
		return exec;
	}

	private static class NodeExec {
		final CompletableFuture<Void> ret;
		String taskRef;

		public NodeExec(CompletableFuture<Void> ret) {
			this.ret = ret;
		}

		public void setTaskRef(String taskRef) {
			this.taskRef = taskRef;
		}
	}

	public static class CyrusContext {
		private final String index;
		public final String dataLocation;

		private CyrusContext(String index, String dataLocation) {
			this.index = index;
			this.dataLocation = dataLocation;
		}

		public static CyrusContext build(BmContext context, String domainUid, String userUid, String folder) {
			ItemValue<Mailbox> box = context.provider().instance(IMailboxes.class, domainUid).getComplete(userUid);
			IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
			String dataLocation = getDataLocation(dir, box);
			MailboxDescriptor mboxDescriptor = new MailboxDescriptor();
			mboxDescriptor.mailboxName = box.value.name;
			mboxDescriptor.type = Mailbox.Type.user;
			mboxDescriptor.utf7FolderPath = UTF7Converter.encode(folder);
			CyrusPartition partition = CyrusPartition.forServerAndDomain(dataLocation, domainUid);
			String indexPath = CyrusFileSystemPathHelper.getMetaFileSystemPath(domainUid, mboxDescriptor, partition,
					"cyrus.index");
			return new CyrusContext(indexPath, dataLocation);
		}

	}

	@FunctionalInterface
	public interface Create {
		public void apply(long conversationId, IInternalMailConversation service, Conversation conversaion);
	}

	@FunctionalInterface
	public interface Update {
		public void apply(IInternalMailConversation service, ItemValue<Conversation> conversaion);
	}

}

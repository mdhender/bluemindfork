/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.conversation.service.tests;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.backend.mail.api.IMailConversationActions;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AbstractConversationTests {
	protected String partition;
	protected String user1Uid;
	protected String user2Uid;
	protected String user1MboxRoot;
	protected String user2MboxRoot;
	protected String domainUid = "test" + System.currentTimeMillis() + ".lab";
	protected DataSource datasource;

	protected IMailboxFolders user1MailboxFolderService;
	protected IMailboxFolders user2MailboxFolderService;

	protected String user1InboxUid;
	protected String user2InboxUid;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1144");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);
		partition = "dataloc__" + domainUid.replace('.', '_');
		datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		JdbcActivator.getInstance().addMailboxDataSource("dataloc", datasource);

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.internal);

		user1Uid = PopulateHelper.addUser("u1-" + System.currentTimeMillis(), domainUid, Routing.internal);
		user2Uid = PopulateHelper.addUser("u2-" + System.currentTimeMillis(), domainUid, Routing.internal);

		user1MboxRoot = "user." + user1Uid.replace('.', '^');
		user2MboxRoot = "user." + user2Uid.replace('.', '^');

		assertNotNull(user1Uid);
		assertNotNull(user2Uid);

		user1MailboxFolderService = provider(user1Uid).instance(IMailboxFolders.class, partition, user1MboxRoot);
		user2MailboxFolderService = provider(user2Uid).instance(IMailboxFolders.class, partition, user2MboxRoot);

		user1InboxUid = user1MailboxFolderService.byName("INBOX").uid;
		user2InboxUid = user2MailboxFolderService.byName("INBOX").uid;

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");
	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IMailConversation getConversationService(String userUid) {
		return provider(userUid).instance(IMailConversation.class,
				SubtreeContainer.mailSubtreeUid(domainUid, Namespace.users, userUid).subtreeUid());
	}

	protected IMailConversationActions getConversationActionsService(String userUid, String mailboxRootUid) {
		return provider(user1Uid).instance(IMailConversationActions.class,
				SubtreeContainer.mailSubtreeUid(domainUid, Namespace.users, userUid).subtreeUid(), mailboxRootUid);
	}

	protected IServiceProvider provider(String userUid) {
		SecurityContext secCtx = new SecurityContext("sid-" + userUid, userUid, Collections.emptyList(),
				Collections.emptyList(), domainUid);
		Sessions.get().put(secCtx.getSessionId(), secCtx);
		return ServerSideServiceProvider.getProvider(secCtx);
	}

	protected IDbMessageBodies getBodies(SecurityContext ctx) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IDbMessageBodies.class, partition);
	}

	protected ReadStream<Buffer> openResource(String path) {
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		return new InputReadStream(inputStream);
	}

	protected Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	protected SortDescriptor createSortDescriptor(ItemFlagFilter flagFilter) {
		SortDescriptor sortDesc = new SortDescriptor();
		sortDesc.filter = flagFilter;
		return sortDesc;
	}

	protected long createEml(String emlPath, String userUid, String mboxRoot, String folderName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(emlPath)) {
			IServiceProvider provider = provider(userUid);
			Stream stream = VertxStream.stream(Buffer.buffer(ByteStreams.toByteArray(in)));
			IMailboxFolders mailboxFolderService = provider.instance(IMailboxFolders.class, partition, mboxRoot);
			ItemValue<MailboxFolder> folder = mailboxFolderService.byName(folderName);
			IMailboxItems mailboxItemService = provider.instance(IMailboxItems.class, folder.uid);
			String partId = mailboxItemService.uploadPart(stream);
			Part fullEml = Part.create(null, "message/rfc822", partId);
			MessageBody messageBody = new MessageBody();
			messageBody.subject = "Subject_" + System.currentTimeMillis();
			messageBody.structure = fullEml;
			MailboxItem item = new MailboxItem();
			item.body = messageBody;
			IOfflineMgmt offlineMgmt = provider.instance(IOfflineMgmt.class, domainUid, userUid);
			IdRange oneId = offlineMgmt.allocateOfflineIds(1);
			long expectedId = oneId.globalCounter;
			mailboxItemService.createById(expectedId, item);
			return expectedId;
		}
	}
}

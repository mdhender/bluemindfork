/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.replication.testhelper.MailboxUniqueId;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;

public abstract class AbstractMailboxRecordsServiceTests<T> {

	protected String mboxUniqueId;
	protected String partition;
	protected MailboxReplicaRootDescriptor mboxDescriptor;
	protected String dom;

	protected Vertx vertx;

	protected ReadStream<Buffer> openResource(String path) {
		InputStream inputStream = AbstractReplicatedMailboxesServiceTests.class.getClassLoader()
				.getResourceAsStream(path);
		Objects.requireNonNull(inputStream, "Failed to open resource @ " + path);
		return new InputReadStream(inputStream);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();
		vertx = VertxPlatform.getVertx();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
		dom = "vagrant" + System.currentTimeMillis() + ".vmw";

		partition = "dataloc__" + dom.replace('.', '_');
		JdbcActivator.getInstance().addMailboxDataSource("dataloc",
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		mboxUniqueId = MailboxUniqueId.random();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;
		BmTestContext testContext = new BmTestContext(securityContext);

		ContainerStore containerHome = new ContainerStore(testContext,
				JdbcTestHelper.getInstance().getMailboxDataDataSource(), securityContext);

		Subtree subtreeId = SubtreeContainer.mailSubtreeUid(dom, Namespace.users, "me");
		ContainerStore dirHome = new ContainerStore(testContext, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		// init a subtree with an inbox
		Container container = Container.create(subtreeId.subtreeUid(), IMailReplicaUids.REPLICATED_MBOXES, "test", "me",
				true);
		String conversationSubtreeUid = IMailReplicaUids.conversationSubtreeUid(dom, "me");
		Container containerConversion = Container.create(conversationSubtreeUid,
				IMailReplicaUids.REPLICATED_CONVERSATIONS, "test", "me", true);
		Container conversionCont = containerHome.create(containerConversion);
		Container acl = Container.create(IMailboxAclUids.uidForMailbox("me"), IMailboxAclUids.MAILBOX_ACL_PREFIX,
				"acls", "me", true);
		acl.domainUid = dom;
		container.domainUid = dom;
		container = containerHome.create(container);
		acl = containerHome.create(acl);
		dirHome.createContainerLocation(container, "dataloc");

		MailboxReplicaStore mboxStore = new MailboxReplicaStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(),
				container, dom);
		ItemStore items = new ItemStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), container,
				securityContext);
		Item mboxRef = items.create(Item.create(mboxUniqueId, null));
		assertNotNull("failed to create replicated mbox item", mboxRef);
		MailboxReplica replica = new MailboxReplica();
		replica.fullName = "INBOX";
		replica.name = "INBOX";
		replica.acls = Collections.emptyList();
		replica.recentTime = replica.lastAppendDate = replica.lastAppendDate = replica.pop3LastLogin = new Date();
		replica.options = "";
		mboxStore.create(mboxRef, replica);

		// for the records
		String containerId = IMailReplicaUids.mboxRecords(mboxUniqueId);
		container = Container.create(containerId, IMailReplicaUids.MAILBOX_RECORDS, "test", "me", true);
		container.domainUid = dom;
		container = containerHome.create(container);

		dirHome.createContainerLocation(conversionCont, "dataloc");
		dirHome.createContainerLocation(container, "dataloc");
		dirHome.createContainerLocation(acl, "dataloc");

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract T getService(SecurityContext ctx);

	protected abstract IDbMessageBodies getBodies(SecurityContext ctx);

}

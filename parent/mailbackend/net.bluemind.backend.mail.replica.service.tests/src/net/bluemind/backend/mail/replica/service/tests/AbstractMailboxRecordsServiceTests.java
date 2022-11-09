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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractMailboxRecordsServiceTests<T> {

	protected String mboxUniqueId;
	protected String mboxUniqueId2;
	protected String partition;
	protected MailboxReplicaRootDescriptor mboxDescriptor;
	protected String domainUid = "domain-" + System.currentTimeMillis() + ".test";
	protected DataSource datasource;

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

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server pipo = new Server();
		pipo.ip = new BmConfIni().get("imap-role");
		pipo.tags = Collections.singletonList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", pipo);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.internal);
		partition = "dataloc__" + domainUid.replace('.', '_');
		datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		JdbcActivator.getInstance().addMailboxDataSource("dataloc", datasource);
		String userUid = PopulateHelper.addUser("me", domainUid, Routing.internal);
		SecurityContext secCtx = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		IMailboxFolders mailboxFolderService = ServerSideServiceProvider.getProvider(secCtx)
				.instance(IMailboxFolders.class, partition, "user." + userUid.replace(".", "^"));
		ItemValue<MailboxFolder> folder = mailboxFolderService.byName("INBOX");
		mboxUniqueId = folder.uid;
		mboxUniqueId2 = mailboxFolderService.byName("Sent").uid;
		assertNotNull(userUid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract T getService(SecurityContext ctx);

	protected abstract IDbMessageBodies getBodies(SecurityContext ctx);

	protected Date adaptDate(int daysBeforeNow) {
		LocalDate localDate = LocalDate.now();
		LocalDate adapted = localDate.minusDays(daysBeforeNow);
		return Date.from(adapted.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

}

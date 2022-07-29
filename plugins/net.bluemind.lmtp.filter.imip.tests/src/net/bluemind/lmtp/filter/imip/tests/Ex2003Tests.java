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
package net.bluemind.lmtp.filter.imip.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.lmtp.filter.imip.ImipFilter;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class Ex2003Tests {
	private String domainUid = "domain.lan";
	private BmContext testContext;
	private String user1Uid;
	private ItemValue<User> user1;
	private ItemValue<Mailbox> user1Mailbox;
	private ICalendar user1Calendar;
	private ItemValue<Domain> domain;
	private ZoneId defaultTz = ZoneId.systemDefault();
	private ZoneId utcTz = ZoneId.of("UTC");

	@Rule
	public final TestName name = new TestName();

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("IP " + esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		new CyrusService(cyrusIp).reload();

		PopulateHelper.addDomainAdmin("admin", domainUid, Mailbox.Routing.internal);

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		user1Uid = PopulateHelper.addUser("user1", domainUid);
		user1 = testContext.provider().instance(IUser.class, domainUid).getComplete(user1Uid);
		user1Mailbox = testContext.provider().instance(IMailboxes.class, domainUid).getComplete(user1Uid);
		user1Calendar = testContext.provider().instance(ICalendar.class, ICalendarUids.defaultUserCalendar(user1Uid));

		domain = testContext.provider().instance(IDomains.class).get(domainUid);
		System.out.println("test setup is complete for " + name.getMethodName());
	}

	@After
	public void after() throws Exception {
		System.err.println("ending " + name.getMethodName());
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHandleExchange2003Invite() {
		ImipFilter filter = new ImipFilter();
		System.err.println("ex2003 request filtering starts...");
		try (InputStream in = Ex2003Tests.class.getClassLoader().getResourceAsStream("ics/invit_2003.eml");
				Message parsed = Mime4JHelper.parse(in)) {
			LmtpEnvelope envel = new LmtpEnvelope();
			envel.addRecipient(new LmtpAddress("<" + user1.value.defaultEmailAddress() + ">", null, null));
			try (Message updated = filter.filter(envel, parsed, 123456L)) {
				assertNotNull(updated);
				Field eventField = updated.getHeader().getField("X-Bm-Event");
				System.err.println(eventField);
				String seriesUid = eventField.getBody().split(";")[0].trim();
				System.err.println("uid? " + seriesUid);
				ItemValue<VEventSeries> byUid = user1Calendar.getComplete(seriesUid);
				assertEquals("Rencontre EX2003 du canada acentuéee", byUid.displayName);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			fail(e1.getMessage());
		}
	}
}

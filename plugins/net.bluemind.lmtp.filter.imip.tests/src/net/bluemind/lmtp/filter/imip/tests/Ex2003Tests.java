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
import java.util.Collections;
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

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.filter.testhelper.EnvelopeBuilder;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.filter.imip.ImipFilter;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
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
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("IP " + esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, pipo);

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.createTestDomain(domainUid, esServer, pipo);

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
			LmtpEnvelope envel = EnvelopeBuilder.forEmails(user1.value.defaultEmailAddress());

			try (Message updated = filter.filter(envel, parsed)) {
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

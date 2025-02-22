/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.service.eventdeferredaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.helper.mail.EventMailHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class EventDeferredActionExecutorTests {

	private static final String domainUid = "defbm.lan";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server imapServer = new Server();
		imapServer.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(imapServer, esServer);

		PopulateHelper.createTestDomain(domainUid, imapServer, esServer);

		PopulateHelper.addUser("testuser", domainUid);
		PopulateHelper.addUser("participant1", domainUid);

	}

	@After
	public void teardown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void executeSingleDeferredAction() throws Exception {
		ICalendar calendar = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		final ZonedDateTime eventDate = ZonedDateTime.now().plusNanos(2000000000);

		EventCreator.defaultVEvent(eventDate).withAlarm(-1).saveOnCalendar(calendar);

		MockedSendmail mailer = new MockedSendmail();
		EventDeferredActionExecutor executor = new EventDeferredActionExecutor(new EventMailHelper(mailer));

		assertEquals(1, getDeferredActions(eventDate).size());
		executor.execute(eventDate);
		long time = System.currentTimeMillis();
		while (!mailer.hasBeenCalled() && System.currentTimeMillis() - time < 10000) {
			Thread.sleep(100);
		}
		assertTrue(mailer.hasBeenCalled());
		assertEquals(0, getDeferredActions(eventDate).size());
		System.err.println("Took " + (System.currentTimeMillis() - time) + "ms to occur");
	}

	@Test
	public void executingSingleDeferredActionWithNoAssociatedEventShouldGetDeleted() throws Exception {
		ICalendar calendar = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		final ZonedDateTime eventDate = ZonedDateTime.now().plusNanos(2000000000);

		String uid = EventCreator.defaultVEvent(eventDate).withAlarm(-1).saveOnCalendar(calendar);

		MockedSendmail mailer = new MockedSendmail();
		EventDeferredActionExecutor executor = new EventDeferredActionExecutor(new EventMailHelper(mailer));

		assertEquals(1, getDeferredActions(eventDate).size());

		calendar.delete(uid, false);
		executor.execute(eventDate);
		long time = System.currentTimeMillis();
		while (!mailer.hasBeenCalled() && System.currentTimeMillis() - time < 3000) {
			Thread.sleep(100);
		}
		assertFalse(mailer.hasBeenCalled());
		assertEquals(0, getDeferredActions(eventDate).size());
	}

	@Test
	public void createNextDeferredAction() throws Exception {
		ICalendar calendar = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				ICalendarUids.defaultUserCalendar("testuser"));

		final ZonedDateTime eventDate = ZonedDateTime.now().plusNanos(2000000000).truncatedTo(ChronoUnit.MILLIS);

		int trigger = -1;
		EventCreator.defaultVEvent(eventDate).withRecurrence(VEvent.RRule.Frequency.DAILY).withAlarm(trigger)
				.saveOnCalendar(calendar);

		MockedSendmail mailer = new MockedSendmail();
		EventDeferredActionExecutor executor = new EventDeferredActionExecutor(new EventMailHelper(mailer));

		List<ItemValue<DeferredAction>> beforeExecute = getDeferredActions(eventDate.plusDays(1));
		assertEquals(1, beforeExecute.size());
		assertEquals(eventDate.plusSeconds(trigger),
				ZonedDateTime.ofInstant(beforeExecute.get(0).value.executionDate.toInstant(), ZoneId.systemDefault()));

		executor.execute(eventDate);
		Thread.sleep(1001);
		List<ItemValue<DeferredAction>> afterExecute = getDeferredActions(eventDate.plusDays(1));
		assertEquals(1, afterExecute.size());
		assertEquals(eventDate.plusDays(1).plusSeconds(trigger).truncatedTo(ChronoUnit.SECONDS),
				ZonedDateTime.ofInstant(afterExecute.get(0).value.executionDate.toInstant(), ZoneId.systemDefault()));
	}

	private IDeferredAction getService() {
		String containerUid = IDeferredActionContainerUids.uidForDomain(domainUid);
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		return provider.instance(IDeferredAction.class, containerUid);
	}

	private List<ItemValue<DeferredAction>> getDeferredActions(ZonedDateTime executionDate) {
		IDeferredAction deferredActionService = getService();
		return deferredActionService.getByActionId(EventDeferredAction.ACTION_ID,
				executionDate.toInstant().toEpochMilli());
	}

}

class MockedSendmail implements ISendmail {

	private boolean wasCalled;

	public MockedSendmail() {
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, Message m) {
		wasCalled();
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(Mail m) {
		wasCalled();
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(Mailbox from, Message m) {
		// TODO Write a test to check Message and Mailbox
		wasCalled();
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String domainUid, Message m) {
		wasCalled();
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			Message m) {
		wasCalled();
		return SendmailResponse.success();
	}

	private void wasCalled() {
		wasCalled = true;
	}

	public boolean hasBeenCalled() {
		return wasCalled;
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream) {
		return send(creds, fromEmail, userDomain, rcptTo, inStream, false);
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			InputStream inStream, boolean requestDSN) {
		wasCalled();
		SendmailResponse sendmailResponse = SendmailResponse.success();
		if (requestDSN) {
			sendmailResponse.requestDSN();
		}
		return sendmailResponse;
	}
}

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filter.testhelper.EnvelopeBuilder;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.filter.imip.IIMIPHandler;
import net.bluemind.lmtp.filter.imip.TodoCancelHandler;
import net.bluemind.lmtp.filter.imip.TodoReplyHandler;
import net.bluemind.lmtp.filter.imip.TodoRequestHandler;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ImipFilterVTodoTests {
	private String domainUid = "domain.lan";
	private ItemValue<User> admin;
	private ItemValue<Mailbox> adminMailbox;
	private ITodoList adminTodolist;

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

		// FIXME bm/es for todo indexing?
		esServer.tags = Lists.newArrayList("bm/es");

		Server pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(esServer, pipo);

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.createTestDomain(domainUid, esServer, pipo);

		PopulateHelper.addDomainAdmin("admin", domainUid, Mailbox.Routing.internal);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		admin = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid)
				.byLogin("admin");
		adminMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(admin.uid);

		adminTodolist = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITodoList.class,
				"todolist:default_admin");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void requestHandler() throws Exception {
		IIMIPHandler handler = new TodoRequestHandler(null, null);

		ItemValue<VTodo> event = defaultVTodo();

		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("admin@domain.lan");

		ElasticsearchTestHelper.getInstance().refresh("todo");
		ItemValue<VTodo> todos = adminTodolist.getComplete(event.uid);

		handler.handle(imip, recipient, null, adminMailbox);
		ElasticsearchTestHelper.getInstance().refresh("todo");
		todos = adminTodolist.getComplete(event.uid);
		assertEquals(event.value.summary, todos.value.summary);
		assertEquals(2, todos.value.attendees.size());

		todos.value.summary = "updated";
		imip.iCalendarElements = Arrays.asList(todos.value);
		imip.sequence = 2;
		handler.handle(imip, recipient, null, adminMailbox);

		ElasticsearchTestHelper.getInstance().refresh("todo");
		ItemValue<VTodo> todo = adminTodolist.getComplete(event.uid);
		assertNotNull(todo);
		assertEquals("updated", todo.value.summary);
		assertEquals(2, todo.value.attendees.size());

	}

	@Test
	public void requestHandlerResource() throws Exception {
		ItemValue<ResourceDescriptor> resource = createResource();
		ItemValue<Mailbox> resourceMailbox = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid).getComplete(resource.uid);

		IIMIPHandler handler = new TodoRequestHandler(null, null);

		ItemValue<VTodo> event = defaultVTodo();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);
		List<Attendee> attendees = new ArrayList<>(event.value.attendees.size());
		for (Attendee a : event.value.attendees) {
			if (!a.mailto.equals("admin@" + domainUid)) {
				attendees.add(a);
			}
		}
		attendees.add(ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
				ICalendarElement.Role.Chair, ICalendarElement.ParticipationStatus.NeedsAction, true, "", "", "",
				resource.value.label, "", "", null, resource.value.emails.iterator().next().address));
		event.value.attendees = attendees;

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail(resource.value.emails.iterator().next().address);

		try {
			handler.handle(imip, recipient, null, resourceMailbox);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertTrue(sf.getMessage()
					.contains("Unsuported VTodo for recipient: " + resource.value.emails.iterator().next().address
							+ ", kind: " + resourceMailbox.value.type.toString()));
		}
	}

	private ItemValue<ResourceDescriptor> createResource() throws ServerFault {
		String resourceUid = "resource-uuid";
		ResourceDescriptor r = new ResourceDescriptor();
		r.label = "resource";
		r.emails = Arrays.asList(Email.create(r.label + "@" + domainUid, true));
		r.typeIdentifier = "default";
		r.dataLocation = new BmConfIni().get("imap-role");

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IResources.class, domainUid)
				.create(resourceUid, r);
		return ItemValue.create(Item.create(resourceUid, ""), r);
	}

	@Test
	public void cancelHandler() throws Exception {
		IIMIPHandler handler = new TodoRequestHandler(null, null);

		ItemValue<VTodo> event = defaultVTodo();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("admin@domain.lan");

		ElasticsearchTestHelper.getInstance().refresh("todo");
		ItemValue<VTodo> todo = adminTodolist.getComplete(event.uid);
		assertNull(todo);
		handler.handle(imip, recipient, null, adminMailbox);

		ElasticsearchTestHelper.getInstance().refresh("todo");
		todo = adminTodolist.getComplete(event.uid);
		assertNotNull(todo);

		IIMIPHandler cancelHandler = new TodoCancelHandler(recipient, null);

		imip.method = ITIPMethod.CANCEL;
		cancelHandler.handle(imip, recipient, null, adminMailbox);
		todo = adminTodolist.getComplete(event.uid);
		assertNull(todo);
	}

	@Test
	public void replyHandler() throws Exception {
		IIMIPHandler handler = new TodoRequestHandler(null, null);

		ItemValue<VTodo> event = defaultVTodo();
		IMIPInfos imip = imip(ITIPMethod.REQUEST, defaultExternalSenderVCard(), event.uid);

		imip.iCalendarElements = Arrays.asList(event.value);
		ResolvedBox recipient = EnvelopeBuilder.lookupEmail("admin@domain.lan");

		ElasticsearchTestHelper.getInstance().refresh("todo");
		ItemValue<VTodo> todo = adminTodolist.getComplete(event.uid);
		assertNull(todo);

		handler.handle(imip, recipient, null, adminMailbox);

		ElasticsearchTestHelper.getInstance().refresh("todo");

		List<ICalendarElement.Attendee> attendees = new ArrayList<>(1);
		ICalendarElement.Attendee org = ICalendarElement.Attendee.create(ICalendarElement.CUType.Individual, "",
				ICalendarElement.Role.Chair, ICalendarElement.ParticipationStatus.Declined, true, "", "", "",
				"external", "", "", null, "external@ext-domain.lan");
		attendees.add(org);
		imip.attendees(attendees);

		ElasticsearchTestHelper.getInstance().refresh("todo");
		imip.method = ITIPMethod.REPLY;
		IIMIPHandler replyHandler = new TodoReplyHandler(null, null);
		replyHandler.handle(imip, recipient, null, adminMailbox);

		ElasticsearchTestHelper.getInstance().refresh("todo");
		todo = adminTodolist.getComplete(event.uid);
		assertEquals(2, todo.value.attendees.size());

		boolean found = false;
		for (ICalendarElement.Attendee attendee : todo.value.attendees) {
			if (attendee.mailto.equals("external@ext-domain.lan")) {
				assertEquals(VTodo.ParticipationStatus.Declined, attendee.partStatus);
				found = true;
			}
		}

		assertTrue(found);
	}

	protected ItemValue<VTodo> defaultVTodo(String uid) {
		VTodo todo = new VTodo();
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		todo.organizer = new VTodo.Organizer("external@ext-domain.lan");

		List<VTodo.Attendee> attendees = new ArrayList<>(1);
		VTodo.Attendee org = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "external", "", "", null,
				"external@ext-domain.lan");
		attendees.add(org);

		VTodo.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.NeedsAction, true, "", "", "", "admin", "", "", null, "admin@domain.lan");
		attendees.add(me);

		todo.attendees = attendees;

		return ItemValue.create(uid, todo);
	}

	protected ItemValue<VTodo> defaultVTodo() {
		return defaultVTodo(UUID.randomUUID().toString());
	}

	private VCard defaultExternalSenderVCard() {
		VCard sender = new VCard();
		sender.identification = new VCard.Identification();
		sender.identification.formatedName = VCard.Identification.FormatedName.create("external",
				Arrays.<VCard.Parameter>asList());
		sender.communications.emails = Arrays
				.asList(VCard.Communications.Email.create("external@ext-domain.lan", Arrays.<VCard.Parameter>asList()));
		return sender;
	}

	private IMIPInfos imip(ITIPMethod method, VCard sender, String uid) {

		IMIPInfos imip = new IMIPInfos();
		imip.method = method;
		imip.messageId = UUID.randomUUID().toString();
		imip.organizerEmail = "external@ext-domain.lan";
		imip.uid = uid;
		imip.sequence = 0;

		return imip;
	}
}

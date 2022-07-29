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
package net.bluemind.calendar.service;

import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.addressbook.domainbook.DomainAddressBook;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.service.internal.CalendarService;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlog.IAuditManager;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryHandlers;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.group.api.Group;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.TagStore;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public abstract class AbstractCalendarTests {

	protected static final String DOMAIN_EXTERNAL_URL = "my.test.domain.external.url";
	protected static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	protected IDomainSettings domainSettings;
	protected ISystemConfiguration systemConfiguration;

	protected TransportClient esearchClient;

	protected ItemValue<User> testUser;
	protected SecurityContext userSecurityContext;
	protected Container userCalendarContainer;
	protected Container userCalendarViewContainer;
	protected Container userFreebusyContainer;

	protected Container userTagContainer;

	protected ItemValue<User> attendee1;
	protected SecurityContext attendee1SecurityContext;
	protected Container attendee1CalendarContainer;
	protected Container attendee1TagContainer;

	protected ItemValue<User> attendee2;
	protected SecurityContext attendee2SecurityContext;
	protected Container attendee2CalendarContainer;
	protected Container attendee2TagContainer;

	protected String member1Uid;

	protected ItemValue<User> forbidden;
	protected Container forbiddenCalendarContainer;

	protected Tag tag1;
	protected TagRef tagRef1;
	protected Tag tag2;
	protected TagRef tagRef2;

	protected ItemValue<VCard> dlistItemValue;
	protected Group group;
	protected String groupUid;
	protected boolean sendNotifications = false;

	protected String domainUid;

	private MailboxStoreService mailboxStore;

	protected ZoneId tz = ZoneId.of("Europe/Paris");
	protected ZoneId utcTz = ZoneId.of("UTC");
	protected ZoneId defaultTz = ZoneId.systemDefault();

	protected BmTestContext testContext = new BmTestContext(SecurityContext.SYSTEM);

	protected SecurityContext basicUserSecurityContext;
	protected DataSource dataDataSource;
	protected DataSource systemDataSource;

	protected AclStore aclStore;
	protected AclStore aclStoreData;

	protected Container domainContainer;
	protected String datalocation;

	private Container userDefActionContainer;

	@Rule
	public final TestName junitName = new TestName();

	@Before
	public void beforeBefore() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList("filehosting/data");

		PopulateHelper.initGlobalVirt(esServer, nodeServer);

		domainUid = "bm.lan";
		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);
		systemDataSource = JdbcTestHelper.getInstance().getDataSource();

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid);

		ContainerStore containerStore = new ContainerStore(testContext, systemDataSource, SecurityContext.SYSTEM);

		Container mboxContainer = containerStore.get(domainUid);
		assertNotNull(mboxContainer);

		mailboxStore = new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM,
				mboxContainer);

		// User container
		Container usersBook = Container.create("addressbook_" + domainUid, "addressbook", domainUid + " users", "me",
				true);
		usersBook = containerStore.get(DomainAddressBook.getIdentifier("bm.lan"));

		domainContainer = containerStore.get("bm.lan");
		assertNotNull(domainContainer);
		ItemStore userItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer,
				SecurityContext.SYSTEM);
		ContainerUserStoreService userStore = new ContainerUserStoreService(testContext, domainContainer, domain);

		GroupStore groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer);
		ItemStore groupsItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer,
				SecurityContext.SYSTEM);

		ContainerStoreService<VCard> vcardStore = new ContainerStoreService<VCard>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, usersBook,
				new VCardStore(JdbcTestHelper.getInstance().getDataSource(), usersBook));

		// test user
		testUser = defaultUser("testUser" + System.nanoTime(), "Doe", "John");

		userStore.create(testUser.uid, testUser.value);
		vcardStore.create("user_" + testUser.uid, "John Doe", testUser.value.contactInfos);

		userSecurityContext = new SecurityContext("user", testUser.uid, Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);

		Sessions.get().put(userSecurityContext.getSessionId(), userSecurityContext);

		basicUserSecurityContext = new SecurityContext("fake", "fake", Arrays.<String>asList(),
				Arrays.<String>asList("hasSimpleVideoconferencing"), domainUid);
		Sessions.get().put(basicUserSecurityContext.getSessionId(), basicUserSecurityContext);

		userCalendarContainer = createTestContainer(userSecurityContext, dataDataSource, ICalendarUids.TYPE, "John Doe",
				ICalendarUids.TYPE + ":Default:" + testUser.uid, testUser.uid);
		userCalendarViewContainer = createTestContainer(userSecurityContext, dataDataSource, "calendarview", "views",
				"calendarview:" + testUser.uid, testUser.uid);
		userDefActionContainer = createTestContainer(userSecurityContext, dataDataSource,
				IDeferredActionContainerUids.TYPE, "defActions", IDeferredActionContainerUids.uidForUser(testUser.uid),
				testUser.uid);

		// vFreeBusy is stored in bj...
		userFreebusyContainer = createTestContainer(userSecurityContext, systemDataSource, IFreebusyUids.TYPE,
				"John Doe", IFreebusyUids.getFreebusyContainerUid(testUser.uid), testUser.uid);

		userTagContainer = createTestContainer(userSecurityContext, dataDataSource, ITagUids.TYPE, "tags",
				ITagUids.TYPE + "_" + testUser.uid, testUser.uid);

		Container contactsContainer = createTestContainer(userSecurityContext, dataDataSource, IAddressBookUids.TYPE,
				"My Contacts", "book:Contacts_" + testUser.uid, testUser.uid);

		Container collectedContactsContainer = createTestContainer(userSecurityContext, dataDataSource,
				IAddressBookUids.TYPE, "Collected contacts yay", "book:CollectedContacts_" + testUser.uid,
				testUser.uid);

		// attendee 1
		attendee1 = defaultUser("test" + UUID.randomUUID().toString(), "attendee1", "attendee1");
		userStore.create(attendee1.uid, attendee1.value);
		vcardStore.create("user_" + attendee1.uid, "John Doe", attendee1.value.contactInfos);
		attendee1SecurityContext = new SecurityContext("attendee1", attendee1.uid, Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		Sessions.get().put(attendee1SecurityContext.getSessionId(), attendee1SecurityContext);
		attendee1CalendarContainer = createTestContainer(attendee1SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"test", ICalendarUids.TYPE + ":Default:" + attendee1.uid, attendee1.uid);
		attendee1TagContainer = createTestContainer(attendee1SecurityContext, dataDataSource, ITagUids.TYPE, "tags",
				ITagUids.TYPE + "_" + attendee1.uid, attendee1.uid);

		// attendee 2
		attendee2 = defaultUser("test" + UUID.randomUUID().toString(), "attendee2", "attendee2");
		userStore.create(attendee2.uid, attendee2.value);
		vcardStore.create("user_" + attendee2.uid, "John Doe", attendee2.value.contactInfos);
		attendee2SecurityContext = new SecurityContext("attendee2", attendee2.uid, Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		Sessions.get().put(attendee2SecurityContext.getSessionId(), attendee2SecurityContext);
		attendee2CalendarContainer = createTestContainer(attendee2SecurityContext, dataDataSource, ICalendarUids.TYPE,
				"test", ICalendarUids.TYPE + ":Default:" + attendee2.uid, attendee2.uid);
		attendee2TagContainer = createTestContainer(attendee2SecurityContext, dataDataSource, ITagUids.TYPE, "tags",
				ITagUids.TYPE + "_" + attendee2.uid, attendee2.uid);

		// forbidden attendee
		forbidden = defaultUser("test" + UUID.randomUUID().toString(), "forbidden", "forbidden");
		userStore.create(forbidden.uid, forbidden.value);
		vcardStore.create("user_" + forbidden.uid, "Forbidden", forbidden.value.contactInfos);
		SecurityContext forbiddenSecurityContext = new SecurityContext("forbidden", forbidden.uid,
				Arrays.<String>asList(), Arrays.<String>asList(), domainUid);
		Sessions.get().put(forbiddenSecurityContext.getSessionId(), forbiddenSecurityContext);
		forbiddenCalendarContainer = createTestContainer(forbiddenSecurityContext, dataDataSource, ICalendarUids.TYPE,
				"test", ICalendarUids.TYPE + ":Default:" + forbidden.uid, forbidden.uid);

		// Dlist
		vcardStore = new ContainerStoreService<VCard>(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, contactsContainer,
				new VCardStore(JdbcTestHelper.getInstance().getDataSource(), contactsContainer));
		VCardIndexStore vcardIndex = new VCardIndexStore(ElasticsearchTestHelper.getInstance().getClient(),
				contactsContainer, null);

		member1Uid = UUID.randomUUID().toString();
		String member1Email = "email" + UUID.randomUUID().toString() + "@vcard.lan";
		VCard member1 = defaultVCard("Member 1", member1Email);
		ItemVersion iv = vcardStore.create(member1Uid, "Member 1", member1);
		vcardIndex.create(Item.create(member1Uid, iv.id), member1);
		String member2Uid = UUID.randomUUID().toString();
		String member2Email = "email" + UUID.randomUUID().toString() + "@vcard.lan";
		VCard member2 = defaultVCard("Member 2", member2Email);
		iv = vcardStore.create(member2Uid, "Member 2", member2);
		vcardIndex.create(Item.create(member2Uid, iv.id), member2);

		VCard dlist = defaultVCard("DLIST", "dlist" + UUID.randomUUID().toString() + "@vcard.lan");
		dlist.kind = Kind.group;
		String dlistUid = UUID.randomUUID().toString();
		dlist.organizational.member = Arrays.asList(
				VCard.Organizational.Member.create(contactsContainer.uid, member1Uid, "Member 1", member1Email),
				VCard.Organizational.Member.create(contactsContainer.uid, member2Uid, "Member 2", member2Email));
		iv = vcardStore.create(dlistUid, "DLIST", dlist);
		vcardIndex.create(Item.create(dlistUid, iv.id), dlist);

		dlistItemValue = vcardStore.get(dlistUid, null);

		// Group
		group = defaultGroup();
		groupUid = UIDGenerator.uid();

		DirEntryHandlers.byKind(DirEntry.Kind.GROUP).create(new BmTestContext(SecurityContext.SYSTEM), "bm.lan",
				DirEntry.create(null, "bm.lan/groups/" + groupUid, DirEntry.Kind.GROUP, groupUid, group.name,
						group.emails.iterator().next().address, false, false, false));
		Item groupItem = groupsItemStore.get(groupUid);
		groupStore.create(groupItem, group);

		group = groupStore.get(groupItem);

		groupStore.addUsersMembers(groupItem, userItemStore.getMultiple(Arrays.asList(attendee1.uid, attendee2.uid)));

		Mailbox groupMbox = new Mailbox();
		groupMbox.type = Type.group;
		groupMbox.routing = Routing.none;
		groupMbox.name = group.name;
		Email e = new Email();
		e.address = group.name + "@bm.lan";
		e.allAliases = true;
		e.isDefault = true;
		groupMbox.emails = Arrays.asList(e);
		mailboxStore.attach(groupUid, null, groupMbox);

		Mailbox groupMailbox = mailboxStore.get(groupUid, null).value;
		group.emails = groupMailbox.emails;

		// Acls
		aclStore = new AclStore(testContext, JdbcTestHelper.getInstance().getDataSource());
		List<AccessControlEntry> ace = Arrays
				.asList(AccessControlEntry.create(userSecurityContext.getSubject(), Verb.All));
		aclStore.store(domainContainer, ace);
		aclStore.store(usersBook, ace);
		aclStore.store(userFreebusyContainer, ace);

		// Acls sharded data
		aclStoreData = new AclStore(testContext, dataDataSource);
		aclStoreData.store(userCalendarContainer, ace);
		aclStoreData.store(userCalendarViewContainer, ace);
		aclStoreData.store(userTagContainer, ace);
		aclStoreData.store(userDefActionContainer, ace);
		aclStoreData.store(contactsContainer, ace);
		aclStoreData.store(collectedContactsContainer, ace);

		List<AccessControlEntry> a1 = Arrays.asList(
				AccessControlEntry.create(attendee1SecurityContext.getSubject(), Verb.All),
				AccessControlEntry.create(attendee2SecurityContext.getSubject(), Verb.Read),
				AccessControlEntry.create(userSecurityContext.getSubject(), Verb.Read));
		aclStoreData.store(attendee1CalendarContainer, a1);
		aclStoreData.store(attendee1TagContainer, a1);

		List<AccessControlEntry> a2 = Arrays
				.asList(AccessControlEntry.create(attendee2SecurityContext.getSubject(), Verb.All));
		aclStoreData.store(attendee2CalendarContainer, a2);
		aclStoreData.store(attendee2TagContainer, a2);

		// Tags
		ContainerStoreService<Tag> storeService = new ContainerStoreService<>(dataDataSource, userSecurityContext,
				userTagContainer, new TagStore(dataDataSource, userTagContainer));

		tag1 = new Tag();
		tag1.label = "tag1";
		tag1.color = "ffffff";
		storeService.create("tag1", "tag1", tag1);
		tagRef1 = new TagRef();
		tagRef1.containerUid = userTagContainer.uid;
		tagRef1.itemUid = "tag1";

		tag2 = new Tag();
		tag2.label = "tag2";
		tag2.color = "ffffff";
		storeService.create("tag2", "tag2", tag2);
		tagRef2 = new TagRef();
		tagRef2.containerUid = userTagContainer.uid;
		tagRef2.itemUid = "tag2";

		// elasticsearch
		esearchClient = ElasticsearchTestHelper.getInstance().getClient();
		System.out.println("vx3 before() " + junitName.getMethodName());
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected VEventSeries defaultVEvent() {
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		return defaultVEvent(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
	}

	/**
	 * @return
	 */
	protected VEventSeries defaultVEvent(ZonedDateTime start) {
		VEventSeries series = new VEventSeries();
		VEvent event = new VEvent();
		event.dtstart = BmDateTimeHelper.time(start);
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;
		event.url = "https://www.bluemind.net";
		event.conference = "https//vi.sio.com/xxx";
		event.conferenceConfiguration.put("conf1", "val1");
		event.conferenceConfiguration.put("conf2", "val2");

		event.attachments = new ArrayList<>();
		AttachedFile attachment1 = new AttachedFile();
		attachment1.publicUrl = "http://somewhere/1";
		attachment1.name = "test.gif";
		attachment1.cid = "cid0123456789";
		event.attachments.add(attachment1);
		AttachedFile attachment2 = new AttachedFile();
		attachment2.publicUrl = "http://somewhere/2";
		attachment2.name = "test.png";
		event.attachments.add(attachment2);

		event.organizer = new VEvent.Organizer(testUser.value.login + "@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "", "osef", null, null, null,
				"external@attendee.lan");
		attendees.add(me);

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>(2);
		event.categories.add(tagRef1);
		event.categories.add(tagRef2);

		series.main = event;
		return series;
	}

	protected VEventOccurrence recurringVEvent() {
		VEventOccurrence event = new VEventOccurrence();
		ZoneId tz = ZoneId.of("Asia/Ho_Chi_Minh");
		event.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 1, 0, 0, 0, tz));
		event.recurid = BmDateTimeHelper.time(ZonedDateTime.of(2022, 2, 13, 2, 0, 0, 0, tz));
		event.summary = "event " + System.currentTimeMillis();
		event.location = "Toulouse";
		event.description = "Lorem ipsum";
		event.transparency = VEvent.Transparency.Opaque;
		event.classification = VEvent.Classification.Private;
		event.status = VEvent.Status.Confirmed;
		event.priority = 3;

		event.organizer = new VEvent.Organizer(testUser.value.login + "@bm.lan");

		List<VEvent.Attendee> attendees = new ArrayList<>(1);
		VEvent.Attendee me = VEvent.Attendee.create(VEvent.CUType.Individual, "", VEvent.Role.Chair,
				VEvent.ParticipationStatus.Accepted, true, "", "", "",
				testUser.value.contactInfos.identification.formatedName.value, null, null, null,
				testUser.value.login + "@bm.lan");
		attendees.add(me);

		event.attendees = attendees;

		event.categories = new ArrayList<TagRef>(2);
		event.categories.add(tagRef1);
		event.categories.add(tagRef2);

		return event;
	}

	private ItemValue<User> defaultUser(String login, String lastname, String firstname) {
		net.bluemind.user.api.User user = new User();
		login = login.toLowerCase();
		user.login = login;
		Email em = new Email();
		em.address = login + "@bm.lan";
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		VCard card = new VCard();
		card.identification.name = Name.create(lastname, firstname, null, null, null, null);
		card.identification.formatedName = VCard.Identification.FormatedName.create(firstname + " " + lastname,
				Arrays.<VCard.Parameter>asList());
		user.contactInfos = card;
		ItemValue<User> ret = ItemValue.create(login + "_bm.lan", user);
		ret.displayName = card.identification.formatedName.value;
		return ret;
	}

	protected Container createTestContainer(SecurityContext context, DataSource datasource, String type, String name,
			String uid, String owner) throws SQLException {
		BmContext ctx = new BmTestContext(context);
		ContainerStore containerHome = new ContainerStore(ctx, datasource, context);
		Container container = Container.create(uid, type, name, owner, domainUid, true);
		container = containerHome.create(container);
		if (datasource != systemDataSource) {
			ContainerStore directoryStore = new ContainerStore(ctx, ctx.getDataSource(), context);
			directoryStore.createOrUpdateContainerLocation(container, datalocation);
		}
		IUserSubscription subApi = ctx.provider().instance(IUserSubscription.class, domainUid);
		subApi.subscribe(context.getSubject(), Arrays.asList(ContainerSubscription.create(container.uid, true)));
		return container;
	}

	private VCard defaultVCard(String formattedName, String email) {
		VCard card = new VCard();
		card.identification = new VCard.Identification();
		card.identification.formatedName = VCard.Identification.FormatedName.create(formattedName,
				Arrays.<VCard.Parameter>asList());
		card.communications.emails = Arrays.asList(VCard.Communications.Email.create(email));
		return card;
	}

	private Group defaultGroup() {
		Group group = new Group();
		group.name = "group-" + System.nanoTime();

		group.description = "Test group";
		Email e = new Email();
		e.address = group.name + "@bm.lan";
		e.allAliases = true;
		e.isDefault = true;
		group.emails = Arrays.asList(e);

		return group;
	}

	protected ICalendar getCalendarService(SecurityContext context, Container container) throws ServerFault {
		BmContext ctx = new BmTestContext(context);
		DataSource ds = DataSourceRouter.get(ctx, container.uid);
		return new CalendarService(ds, esearchClient, container, ctx,
				CalendarAuditor.auditor(IAuditManager.instance(), ctx, container));
	}

	protected Map<String, String> setGlobalExternalUrl() {
		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}

	protected Map<String, String> setDomainExternalUrl() {
		domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class,
				domainUid);
		Map<String, String> domainValues = new HashMap<>();
		domainValues.put(DomainSettingsKeys.external_url.name(), DOMAIN_EXTERNAL_URL);
		domainSettings.set(domainValues);
		return domainValues;
	}

}

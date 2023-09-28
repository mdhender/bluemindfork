/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.persistence.VCardIndexStore;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.addressbook.service.internal.VCardContainerStoreService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ItemValueAuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.ItemTagRef;
import net.bluemind.tag.persistence.TagRefStore;
import net.bluemind.tag.persistence.TagStore;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSubscription;

public abstract class AbstractServiceTests {

	private static Logger logger = LoggerFactory.getLogger(AbstractServiceTests.class);

	protected VCardStore vCardStore;
	protected TagRefStore tagRefStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;
	protected Container container;

	protected ElasticsearchClient esearchClient;

	protected Container domainTagContainer;
	protected Container tagContainer;

	protected Tag tag1;

	protected Tag tag2;

	protected TagRef tagRef1;

	protected TagRef tagRef2;
	protected BmContext context;

	protected String owner;

	protected VCardContainerStoreService cardStoreService;

	protected String domainUid;

	protected String datalocation;

	protected DataSource dataDataSource;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@AfterClass
	public static void afterClass() {
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		domainUid = "bm.lan";
		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);
		PopulateHelper.addDomain(domainUid);

		owner = PopulateHelper.addUser("test", domainUid);

		defaultSecurityContext = BmTestContext.contextWithSession("testUser", "test", domainUid).getSecurityContext();
		context = new BmTestContext(defaultSecurityContext);

		container = createTestContainer(owner);
		initTags(owner);
		itemStore = new ItemStore(dataDataSource, container, defaultSecurityContext);

		vCardStore = new VCardStore(dataDataSource, container);
		tagRefStore = new TagRefStore(dataDataSource, container);
		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM), dataDataSource);
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		IUserSubscription subService = context.provider().instance(IUserSubscription.class, container.domainUid);
		subService.subscribe(defaultSecurityContext.getSubject(),
				Arrays.asList(ContainerSubscription.create(container.uid, true)));

		esearchClient = ElasticsearchTestHelper.getInstance().getClient();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(container.uid, container.name,
				container.owner, container.type, container.domainUid, container.defaultContainer);
		descriptor.internalId = container.id;
		ItemValueAuditLogService<VCard> logService = new ItemValueAuditLogService<>(context.getSecurityContext(),
				descriptor);

		cardStoreService = new VCardContainerStoreService(context, dataDataSource, SecurityContext.SYSTEM, container,
				new VCardStore(dataDataSource, container),
				new VCardIndexStore(ElasticsearchTestHelper.getInstance().getClient(), container, null), logService);
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");
	}

	private void initTags(String userUid) throws SQLException, ServerFault {
		// domain tags container
		ContainerStore containerHome = new ContainerStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);

		String containerId = "tags_" + domainUid;
		domainTagContainer = containerHome.get(containerId);
		if (domainTagContainer == null) {
			domainTagContainer = Container.create(containerId, ITagUids.TYPE, "domain tags", domainUid, domainUid,
					true);
			domainTagContainer = containerHome.create(domainTagContainer);
		}

		// user tags container
		containerHome = new ContainerStore(context, dataDataSource, defaultSecurityContext);

		containerId = "tags_" + userUid;
		tagContainer = containerHome.get(containerId);
		if (tagContainer == null) {
			tagContainer = Container.create(containerId, ITagUids.TYPE, "test", userUid, domainUid, true);
			tagContainer = containerHome.create(tagContainer);
		}

		ContainerStore directoryStore = new ContainerStore(context, context.getDataSource(), defaultSecurityContext);
		directoryStore.createOrUpdateContainerLocation(tagContainer, datalocation);

		AclStore aclStore = new AclStore(new BmTestContext(SecurityContext.SYSTEM), dataDataSource);
		aclStore.store(tagContainer,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		// create some tags
		ContainerStoreService<Tag> storeService = new ContainerStoreService<>(dataDataSource, defaultSecurityContext,
				tagContainer, new TagStore(dataDataSource, container));

		tag1 = new Tag();
		tag1.label = "tag1";
		tag1.color = "ffffff";
		storeService.create("tag1", "tag1", tag1);
		tagRef1 = new TagRef();
		tagRef1.containerUid = tagContainer.uid;
		tagRef1.itemUid = "tag1";

		tag2 = new Tag();
		tag2.label = "tag2";
		tag2.color = "ffffff";
		storeService.create("tag2", "tag2", tag2);
		tagRef2 = new TagRef();
		tagRef2.containerUid = tagContainer.uid;
		tagRef2.itemUid = "tag2";
	}

	protected Container createTestContainer(String userUid) throws SQLException {
		ContainerStore containerHome = new ContainerStore(context, dataDataSource, defaultSecurityContext);

		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, IAddressBookUids.TYPE, "test", userUid, domainUid, true);
		container = containerHome.create(container);
		assertNotNull(container);

		ContainerStore directoryStore = new ContainerStore(context, context.getDataSource(), defaultSecurityContext);
		directoryStore.createOrUpdateContainerLocation(container, datalocation);

		return container;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract IAddressBook getService(SecurityContext context) throws ServerFault;

	protected ItemValue<VCard> createAndGet(String uid, VCard card) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);

			new ChangelogStore(dataDataSource, container)
					.itemCreated(ChangelogStore.LogEntry.create(uid, item.externalId, "test", "junit", item.id, 0));
			vCardStore.create(item, card);
			tagRefStore.create(item, card.explanatory.categories.stream().map(ref -> {
				return ItemTagRef.create(ref.containerUid, ref.itemUid);
			}).collect(Collectors.toList()));
			return ItemValue.create(item, vCardStore.get(item));

		} catch (SQLException e) {
			logger.error("error during vcard persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

	protected String create(VCard card) {
		String uid = "vcarduid_" + System.nanoTime();
		createAndGet(uid, card);
		return uid;
	}

	protected VCard defaultVCard() {
		VCard card = new VCard();

		card.identification = new VCard.Identification();

		card.identification.formatedName = VCard.Identification.FormatedName.create("default",
				Arrays.<VCard.Parameter>asList());

		card.explanatory.categories = Arrays.asList(tagRef1, tagRef2);

		card.related.spouse = "Clara Morgane";
		card.related.assistant = "Sylvain Garcia";
		card.related.manager = "David Phan";

		VCard.Organizational organizational = VCard.Organizational.create("Loser", "Boss", //
				VCard.Organizational.Org.create("Blue-mind", "tlse", "Dev"), //
				Arrays.<VCard.Organizational.Member>asList());

		card.organizational = organizational;

		return card;
	}

}

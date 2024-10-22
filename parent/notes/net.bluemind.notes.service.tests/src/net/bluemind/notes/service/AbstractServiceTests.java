/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.service;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.ItemValueAuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteIndexMgmt;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.INotes;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.persistence.VNoteIndexStore;
import net.bluemind.notes.persistence.VNoteStore;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.state.StateContext;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.TagRefStore;
import net.bluemind.tag.persistence.TagStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractServiceTests {

	protected VNoteStore vnoteStore;
	protected TagRefStore tagRefStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;
	protected Container container;

	protected Container tagContainer;

	protected Tag tag1;
	protected Tag tag2;
	protected TagRef tagRef1;
	protected TagRef tagRef2;

	protected BmContext defaultContext;

	protected VNoteContainerStoreService vnoteStoreService;

	protected String datalocation;
	protected DataSource dataDataSource;
	protected String domainUid = "bm.lan";
	protected String owner;

	protected ElasticsearchClient esearchClient;

	ISystemConfiguration systemConfiguration;
	private static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	@Before
	public void before() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());
		PopulateHelper.initGlobalVirt(esServer);

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);

		PopulateHelper.addDomain(domainUid);
		owner = PopulateHelper.addUser("test", domainUid);

		defaultSecurityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		defaultContext = new BmTestContext(defaultSecurityContext);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		container = createTestContainer();
		initTags();
		itemStore = new ItemStore(dataDataSource, container, defaultSecurityContext);

		vnoteStore = new VNoteStore(dataDataSource, container);
		tagRefStore = new TagRefStore(dataDataSource, container);
		AclStore aclStore = new AclStore(defaultContext, dataDataSource);
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		esearchClient = ElasticsearchTestHelper.getInstance().getClient();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		BaseContainerDescriptor descriptor = BaseContainerDescriptor.create(container.uid, container.name,
				container.owner, container.type, container.domainUid, container.defaultContainer);
		descriptor.internalId = container.id;

		ItemValueAuditLogService<VNote> logService = new ItemValueAuditLogService<>(defaultContext.getSecurityContext(),
				descriptor);

		vnoteStoreService = new VNoteContainerStoreService(defaultContext, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, container, vnoteStore, logService);
		StateContext.setState("core.stopped");
		StateContext.setState("core.started");
		StateContext.setState("core.started");

	}

	private void initTags() throws SQLException, ServerFault {
		ContainerStore containerHome = new ContainerStore(defaultContext, dataDataSource, defaultSecurityContext);

		String containerId = "test_" + System.nanoTime();
		tagContainer = Container.create(containerId, ITagUids.TYPE, "test", owner, domainUid, true);
		tagContainer = containerHome.create(tagContainer);

		containerHome = new ContainerStore(defaultContext, JdbcActivator.getInstance().getDataSource(),
				defaultSecurityContext);
		containerHome.createOrUpdateContainerLocation(tagContainer, datalocation);

		AclStore aclStore = new AclStore(defaultContext, dataDataSource);
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

	protected Container createTestContainer() throws SQLException {
		ContainerStore containerHome = new ContainerStore(defaultContext, dataDataSource, defaultSecurityContext);

		String containerId = "test_" + System.nanoTime();
		Container tcontainer = Container.create(containerId, INoteUids.TYPE, "test", owner, domainUid);
		tcontainer = containerHome.create(tcontainer);
		assertNotNull(tcontainer);

		containerHome = new ContainerStore(new BmTestContext(defaultSecurityContext),
				JdbcActivator.getInstance().getDataSource(), defaultSecurityContext);
		containerHome.createOrUpdateContainerLocation(tcontainer, datalocation);

		return tcontainer;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
	}

	protected abstract INote getServiceNote(SecurityContext context, String containerUid) throws ServerFault;

	protected abstract INoteIndexMgmt getServiceNoteMgmt(SecurityContext context) throws ServerFault;

	protected abstract INotes getServiceNotes(SecurityContext context) throws ServerFault;

	protected VNote defaultVNote() {
		List<TagRef> categories = new ArrayList<>(1);
		categories.add(tagRef1);
		categories.add(tagRef2);

		return defaultVNote(categories);
	}

	protected VNote defaultVNote(List<TagRef> categories) {
		VNote note = new VNote();
		note.subject = "Note " + System.currentTimeMillis();
		note.body = "Content";
		note.height = 25;
		note.width = 42;
		note.posX = 25;
		note.posY = 42;
		note.categories = categories;
		return note;
	}

	protected void refreshIndex() throws ElasticsearchException, IOException {
		esearchClient.indices().refresh(r -> r.index(VNoteIndexStore.VNOTE_WRITE_ALIAS));
	}

	protected void setGlobalExternalUrl() {
		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
	}

}

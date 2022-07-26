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
package net.bluemind.todolist.service;

import static net.bluemind.todolist.persistence.VTodoIndexStore.VTODO_WRITE_ALIAS;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.elasticsearch.client.transport.TransportClient;
import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
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
import net.bluemind.icalendar.api.ICalendarElement.Status;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.tag.persistence.TagRefStore;
import net.bluemind.tag.persistence.TagStore;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.persistence.VTodoStore;
import net.bluemind.todolist.service.internal.VTodoContainerStoreService;

public abstract class AbstractServiceTests {

	protected VTodoStore vtodoStore;
	protected TagRefStore tagRefStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;
	protected Container container;

	protected TransportClient esearchClient;

	protected Container tagContainer;

	protected Tag tag1;

	protected Tag tag2;

	protected TagRef tagRef1;

	protected TagRef tagRef2;

	protected BmContext defaultContext;

	protected ZoneId tz = ZoneId.of("Europe/Paris");
	protected ZoneId utcTz = ZoneId.of("UTC");
	protected VTodoContainerStoreService vtodoStoreService;

	protected String datalocation;
	protected DataSource dataDataSource;
	protected String domainUid;
	protected String owner;

	protected static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";
	protected ISystemConfiguration systemConfiguration;

	@Before
	public void before() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
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

		defaultSecurityContext = new SecurityContext("testUser", "test", Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		defaultContext = new BmTestContext(defaultSecurityContext);

		Sessions.get().put(defaultSecurityContext.getSessionId(), defaultSecurityContext);

		container = createTestContainer();
		initTags();
		itemStore = new ItemStore(dataDataSource, container, defaultSecurityContext);

		vtodoStore = new VTodoStore(dataDataSource, container);
		tagRefStore = new TagRefStore(dataDataSource, container);
		AclStore aclStore = new AclStore(defaultContext, dataDataSource);
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		esearchClient = ElasticsearchTestHelper.getInstance().getClient();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		vtodoStoreService = new VTodoContainerStoreService(defaultContext, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, container, vtodoStore);

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
		Container container = Container.create(containerId, ITodoUids.TYPE, "test", owner, domainUid);
		container = containerHome.create(container);
		assertNotNull(container);

		containerHome = new ContainerStore(new BmTestContext(defaultSecurityContext),
				JdbcActivator.getInstance().getDataSource(), defaultSecurityContext);
		containerHome.createOrUpdateContainerLocation(container, datalocation);

		return container;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract ITodoList getService(SecurityContext context) throws ServerFault;

	protected VTodo defaultVTodo() {
		VTodo todo = new VTodo();
		todo.uid = UUID.randomUUID().toString();
		ZonedDateTime temp = ZonedDateTime.of(2024, 12, 28, 0, 0, 0, 0, ZoneId.of("UTC"));
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.classification = VTodo.Classification.Private;
		todo.status = Status.NeedsAction;
		todo.priority = 3;

		todo.organizer = new VTodo.Organizer("mehdi@bm.lan");

		List<VTodo.Attendee> attendees = new ArrayList<>(2);

		VTodo.Attendee john = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.Chair,
				VTodo.ParticipationStatus.Accepted, true, "", "", "", "John Bang", "", "", "uid1", "john.bang@bm.lan");
		attendees.add(john);

		VTodo.Attendee jane = VTodo.Attendee.create(VTodo.CUType.Individual, "", VTodo.Role.RequiredParticipant,
				VTodo.ParticipationStatus.NeedsAction, true, "", "", "", "Jane Bang", "", "", "uid2",
				"jane.bang@bm.lan");
		attendees.add(jane);

		todo.attendees = attendees;

		todo.attendees = attendees;

		todo.categories = new ArrayList<TagRef>(2);
		todo.categories.add(tagRef1);
		todo.categories.add(tagRef2);
		return todo;
	}

	protected void refreshIndex() {
		esearchClient.admin().indices().prepareRefresh(VTODO_WRITE_ALIAS).get();
	}

	protected Map<String, String> setGlobalExternalUrl() {
		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);
		return sysValues;
	}
}

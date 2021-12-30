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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteIndexMgmt;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.INotes;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNoteQuery;
import net.bluemind.notes.api.VNotesQuery;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class NotesServiceTests extends AbstractServiceTests {

	private ContainerStore containerStore;
	private SecurityContext securityContext;
	private AclStore aclStore;
	private String userUid;
	private UserSubscriptionStore userSubscriptionStore;
	private ZoneId utcTz = ZoneId.of("UTC");

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		containerStore = new ContainerStore(JdbcTestHelper.getInstance().getMailboxDataDataSource(), securityContext);
		ContainerStore dirContainerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		aclStore = new AclStore(JdbcTestHelper.getInstance().getMailboxDataDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);
		PopulateHelper.createTestDomain("bm.lan");
		userUid = PopulateHelper.addUser("test", "bm.lan");

		securityContext = new SecurityContext("testSessionId", userUid, Arrays.<String>asList(),
				Arrays.<String>asList(), "bm.lan");
		Sessions.get().put(securityContext.getSessionId(), securityContext);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		userSubscriptionStore = new UserSubscriptionStore(securityContext, JdbcTestHelper.getInstance().getDataSource(),
				dirContainerStore.get("bm.lan"));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void create() throws Exception {
		List<String> subs = userSubscriptionStore.listSubscriptions(userUid, INoteUids.TYPE);

		Container c = checkContainerCreation();
		assertEquals(subs.size() + 1, userSubscriptionStore.listSubscriptions(userUid, INoteUids.TYPE).size());

		List<AccessControlEntry> acls = aclStore.get(c);
		assertEquals(1, acls.size());

		getServiceNotes(securityContext).delete(c.uid);
	}

	@Test
	public void delete() throws Exception {
		Container c = checkContainerCreation();
		String containerUid = c.uid;

		VNote note = defaultVNote(Collections.emptyList());
		INote serviceNote = getServiceNote(securityContext, containerUid);
		assertNotNull(serviceNote);
		List<ItemValue<VNote>> all = serviceNote.all();
		assertNotNull(all);
		assertTrue(all.isEmpty());
		serviceNote.create(containerUid + "-note", note);

		getServiceNotes(securityContext).delete(containerUid);

		assertNull(containerStore.get(containerUid));
	}

	@Test
	public void testSearch() throws Exception {
		Container c = checkContainerCreation();
		String containerUid = c.uid;

		VNote vnote = defaultVNote(Collections.emptyList());
		vnote.subject = "toto";
		getServiceNote(securityContext, containerUid).create(containerUid + "-note", vnote);

		VNoteQuery vnoteQuery = VNoteQuery.create("value.subject:toto");
		VNotesQuery query = VNotesQuery.create(vnoteQuery, Arrays.asList(containerUid));
		assertEquals(1, getServiceNote(securityContext, containerUid).search(query.vnoteQuery).total);
		List<ItemContainerValue<VNote>> res = getServiceNotes(securityContext).search(query);

		assertEquals(1, res.size());
		VNote found = res.get(0).value;
		assertEquals(vnote.subject, found.subject);

		query = VNotesQuery.create(vnoteQuery, userUid);
		assertEquals(1, getServiceNote(securityContext, containerUid).search(query.vnoteQuery).total);

		res = getServiceNotes(securityContext).search(query);

		assertEquals(1, res.size());
		found = res.get(0).value;
		assertEquals(vnote.subject, found.subject);

		getServiceNotes(securityContext).delete(containerUid);
	}

	private Container checkContainerCreation() throws SQLException {
		String containerUid = UUID.randomUUID().toString();

		ContainerDescriptor cd = new ContainerDescriptor();
		cd.defaultContainer = false;
		cd.domainUid = "bm.lan";
		cd.name = "new container";
		cd.type = INoteUids.TYPE;
		cd.uid = containerUid;
		cd.owner = userUid;

		getServiceNotes(securityContext).create(containerUid, cd);

		Container c = containerStore.get(containerUid);
		assertNotNull(c);
		assertEquals(INoteUids.TYPE, c.type);

		return c;
	}

	@Override
	protected INote getServiceNote(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INote.class, containerUid);
	}

	@Override
	protected INoteIndexMgmt getServiceNoteMgmt(SecurityContext context) throws ServerFault {
		return null;
	}

	@Override
	protected INotes getServiceNotes(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(INotes.class);
	}

}

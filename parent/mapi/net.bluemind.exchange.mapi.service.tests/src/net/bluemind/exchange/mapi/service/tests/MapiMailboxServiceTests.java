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
package net.bluemind.exchange.mapi.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.IMapiMailboxes;
import net.bluemind.exchange.mapi.api.MapiFAI;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class MapiMailboxServiceTests {

	protected String domainUid;
	protected SecurityContext userSecurityContext;
	protected String userUid;
	protected ItemValue<Mailbox> mailbox;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = ini.get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "bmtest.lan";
		PopulateHelper.addDomain(domainUid, Routing.none);

		userUid = PopulateHelper.addUser("test", domainUid, Routing.internal);

		userSecurityContext = new SecurityContext("testSessionId", userUid, Arrays.<String>asList(),
				Arrays.<String>asList(), domainUid);
		IMailboxes mboxesApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domainUid);
		this.mailbox = mboxesApi.byEmail(userUid + "@" + domainUid);
		assertNotNull(mailbox);
		System.err.println("mailbox is " + mailbox);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IMapiFolderAssociatedInformation faisApi(String replicaUid) {
		IMapiFolderAssociatedInformation service = ServerSideServiceProvider.getProvider(userSecurityContext)
				.instance(IMapiFolderAssociatedInformation.class, replicaUid);
		return service;
	}

	protected IOfflineMgmt offlineApi() {
		IOfflineMgmt service = ServerSideServiceProvider.getProvider(userSecurityContext).instance(IOfflineMgmt.class,
				domainUid, mailbox.uid);
		return service;
	}

	protected IMapiMailbox mapiMboxApi() {
		IMapiMailbox service = ServerSideServiceProvider.getProvider(userSecurityContext).instance(IMapiMailbox.class,
				domainUid, mailbox.uid);
		return service;
	}

	protected IMapiMailboxes mapiMboxesApi() {
		IMapiMailboxes service = ServerSideServiceProvider.getProvider(userSecurityContext)
				.instance(IMapiMailboxes.class, domainUid);
		return service;
	}

	protected IContainersFlatHierarchy hierarchyApi() {
		return ServerSideServiceProvider.getProvider(userSecurityContext).instance(IContainersFlatHierarchy.class,
				domainUid, userUid);
	}

	@Test
	public void testGetService() {
		assertNotNull(mapiMboxApi());
	}

	@Test
	public void testCreateGet() {
		IMapiMailbox api = mapiMboxApi();
		MapiReplica replica = api.get();
		assertNull(replica);
		replica = new MapiReplica();
		replica.localReplicaGuid = "local";
		replica.logonReplicaGuid = "logon";
		replica.mailboxGuid = "mboxGuid";
		replica.mailboxUid = mailbox.uid;
		api.create(replica);
		MapiReplica found = api.get();
		assertNotNull(found);

		IMapiMailboxes mboxesApi = mapiMboxesApi();
		MapiReplica reFound = mboxesApi.byMailboxGuid(replica.mailboxGuid);
		assertEquals(replica.mailboxUid, reFound.mailboxUid);

		api.check();

		IContainersFlatHierarchy hierApi = hierarchyApi();
		List<ItemValue<ContainerHierarchyNode>> containers = hierApi.list();
		assertNotNull(containers);
		Optional<ItemValue<ContainerHierarchyNode>> faiContainerFound = containers.stream()
				.filter(iv -> iv.value.containerType.equals(MapiFAIContainer.TYPE)).findAny();
		assertTrue(faiContainerFound.isPresent());
		ItemValue<ContainerHierarchyNode> item = faiContainerFound.get();
		ItemValue<ContainerHierarchyNode> byId = hierApi.getCompleteById(item.internalId);
		assertNotNull(byId);
	}

	@Test
	public void testFais() {
		IMapiMailbox api = mapiMboxApi();
		MapiReplica replica = api.get();
		assertNull(replica);
		replica = new MapiReplica();
		replica.localReplicaGuid = "local";
		replica.logonReplicaGuid = "logon";
		replica.mailboxGuid = "mboxGuid";
		replica.mailboxUid = mailbox.uid;
		api.create(replica);
		MapiReplica found = api.get();
		assertNotNull(found);

		IMapiFolderAssociatedInformation faiApi = faisApi(replica.localReplicaGuid);
		IOfflineMgmt ids = offlineApi();
		int allocCnt = 250;
		IdRange alloc = ids.allocateOfflineIds(allocCnt);
		String[] folders = new String[] { "f1", "f2", "f3", "f4" };
		for (int i = 0; i < alloc.count; i++) {
			long itemid = alloc.globalCounter + i;
			MapiFAI fai = new MapiFAI();
			fai.folderId = folders[(int) itemid % folders.length];
			fai.faiJson = "{}";
			faiApi.store(itemid, fai);
		}
		int total = 0;
		for (int i = 0; i < folders.length; i++) {
			List<ItemValue<MapiFAI>> result = faiApi.getByFolderId(folders[i]);
			total += result.size();
			System.err.println("Got " + result.size() + " for " + folders[i]);
		}
		total += faiApi.getByFolderId("fake").size();
		assertEquals(allocCnt, total);

	}

	@Test
	public void testDelete() {
		IMapiMailbox api = mapiMboxApi();
		MapiReplica replica = api.get();
		assertNull(replica);
		replica = new MapiReplica();
		replica.localReplicaGuid = "local";
		replica.logonReplicaGuid = "logon";
		replica.mailboxGuid = "mboxGuid";
		replica.mailboxUid = mailbox.uid;
		api.create(replica);
		MapiReplica found = api.get();
		assertNotNull(found);

		IMapiMailboxes mboxesApi = mapiMboxesApi();
		MapiReplica reFound = mboxesApi.byMailboxGuid(replica.mailboxGuid);
		assertEquals(replica.mailboxUid, reFound.mailboxUid);

		api.delete();

		assertNull(api.get());
	}

}

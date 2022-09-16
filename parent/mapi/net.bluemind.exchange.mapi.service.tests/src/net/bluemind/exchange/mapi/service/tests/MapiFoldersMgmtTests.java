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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiRawMessage;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MapiFoldersMgmtTests {

	protected String domainUid;
	protected SecurityContext userSecurityContext;
	protected String userUid;
	protected ItemValue<Mailbox> mailbox;
	private MapiReplica replica;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

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
		IMapiMailbox mapiMboxApi = ServerSideServiceProvider.getProvider(userSecurityContext)
				.instance(IMapiMailbox.class, domainUid, mailbox.uid);
		MapiReplica mr = new MapiReplica();
		mr.localReplicaGuid = "localRepl";
		mapiMboxApi.create(mr);
		this.replica = mapiMboxApi.get();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IMapiFoldersMgmt mapiFoldersApi() {
		IMapiFoldersMgmt service = ServerSideServiceProvider.getProvider(userSecurityContext)
				.instance(IMapiFoldersMgmt.class, domainUid, mailbox.uid);
		return service;
	}

	protected IContainersFlatHierarchy hierarchyApi() {
		return ServerSideServiceProvider.getProvider(userSecurityContext).instance(IContainersFlatHierarchy.class,
				domainUid, userUid);
	}

	@Test
	public void testGetService() {
		assertNotNull(mapiFoldersApi());
	}

	@Test
	public void testCreate() {
		IMapiFoldersMgmt folders = mapiFoldersApi();
		MapiFolder mf = new MapiFolder();
		mf.replicaGuid = replica.localReplicaGuid;
		mf.containerUid = "mapi-" + mf.replicaGuid + ":oxsfld:root";
		mf.displayName = "";
		mf.parentContainerUid = mf.containerUid;
		folders.store(mf);
	}

	@Test
	public void testDelete() {

		IMapiFoldersMgmt folders = mapiFoldersApi();
		MapiFolder mf = new MapiFolder();
		mf.replicaGuid = replica.localReplicaGuid;
		mf.containerUid = "mapi-" + mf.replicaGuid + ":oxsfld:root";
		mf.displayName = "";
		mf.parentContainerUid = mf.containerUid;
		folders.store(mf);

		ContainerDescriptor fais = ContainerDescriptor.create(mf.containerUid, mf.displayName, replica.mailboxUid,
				MapiFolderContainer.TYPE, domainUid, false);
		assertNotNull(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
				.get(fais.uid));

		IMapiFolder service = ServerSideServiceProvider.getProvider(userSecurityContext).instance(IMapiFolder.class,
				mf.containerUid);

		MapiRawMessage raw = new MapiRawMessage();
		String dn = "xxx" + System.currentTimeMillis();
		JsonObject js = new JsonObject().put("PidTagDisplayName", dn);
		raw.contentJson = js.encode();

		IOfflineMgmt idAllocator = ServerSideServiceProvider.getProvider(userSecurityContext)
				.instance(IOfflineMgmt.class, domainUid, userUid);
		IdRange oneId = idAllocator.allocateOfflineIds(1);

		Ack version = service.createById(oneId.globalCounter, raw);
		assertTrue(version.version > 0);
		ItemValue<MapiRawMessage> found = service.getCompleteById(oneId.globalCounter);
		assertEquals(found.internalId, oneId.globalCounter);
		assertEquals(found.version, version.version);

		Count count = service.count(ItemFlagFilter.all());
		assertEquals(1, count.total);

		folders.deleteAll();
		count = service.count(ItemFlagFilter.all());
		assertEquals(0, count.total);

		assertNull(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
				.getIfPresent(fais.uid));
	}

}

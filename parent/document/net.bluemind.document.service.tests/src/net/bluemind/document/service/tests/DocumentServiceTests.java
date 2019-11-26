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
package net.bluemind.document.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.document.api.Document;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.api.IDocument;
import net.bluemind.document.service.internal.DocumentService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DocumentServiceTests {

	private SecurityContext context;

	protected Container container;
	protected Item item;

	public IDocument getService(SecurityContext sc) throws ServerFault {
		return new DocumentService(new BmTestContext(sc), container, item);

	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		PopulateHelper.initGlobalVirt();

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		context = new SecurityContext("admin", "admin_bm.lan", Arrays.<String>asList(), Arrays.<String>asList(),
				"bm.lan");

		Sessions.get().put(context.getSessionId(), context);

		PopulateHelper.createTestDomain("bm.lan");
		PopulateHelper.addDomainAdmin("admin", "bm.lan");

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), context);

		String containerId = "test" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", "bm.lan", true);
		container = containerHome.create(container);
		container = containerHome.get(containerId);

		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, context);

		item = Item.create("test", UUID.randomUUID().toString());
		itemStore.create(item);
		item = itemStore.get("test");

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(container, Arrays.asList(AccessControlEntry.create(context.getSubject(), Verb.All)));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws Exception {
		InputStream is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");

		String uid = UUID.randomUUID().toString();
		Document doc = new Document();
		doc.content = ByteStreams.toByteArray(is);

		DocumentMetadata meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "wat.jpg";
		meta.mime = "image/jpg";
		meta.name = "wat ??";
		meta.description = "description wat ??";

		doc.metadata = meta;

		getService(context).create(uid, doc);

		DocumentMetadata fetchedMetadata = getService(context).fetchMetadata(uid);
		assertEquals(meta, fetchedMetadata);

		Document fetched = getService(context).fetch(uid);

		assertTrue(Arrays.equals(doc.content, fetched.content));
	}

	@Test
	public void testUpdate() throws Exception {
		InputStream is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");

		String uid = UUID.randomUUID().toString();
		Document doc = new Document();
		doc.content = ByteStreams.toByteArray(is);

		DocumentMetadata meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "wat.jpg";
		meta.mime = "image/jpg";
		meta.name = "wat ??";
		meta.description = "description wat ??";

		doc.metadata = meta;

		getService(context).create(uid, doc);

		// update
		is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/panda.jpg");
		doc.content = ByteStreams.toByteArray(is);
		meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "panda.jpg";
		meta.mime = "image/jpg";
		meta.name = "pandipanda ??";
		meta.description = "description pandipanda ??";

		doc.metadata = meta;

		getService(context).update(uid, doc);

		// fetch updated doc

		DocumentMetadata fetchedMetadata = getService(context).fetchMetadata(uid);
		assertEquals(meta, fetchedMetadata);

		Document fetched = getService(context).fetch(uid);

		assertTrue(Arrays.equals(doc.content, fetched.content));

	}

	@Test
	public void testDelete() throws Exception {
		InputStream is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");

		String uid = UUID.randomUUID().toString();
		Document doc = new Document();
		doc.content = ByteStreams.toByteArray(is);

		DocumentMetadata meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "wat.jpg";
		meta.mime = "image/jpg";
		meta.name = "wat ??";
		meta.description = "description wat ??";

		doc.metadata = meta;

		getService(context).create(uid, doc);

		getService(context).delete(uid);
		DocumentMetadata fetchedMetadata = getService(context).fetchMetadata(uid);
		assertNull(fetchedMetadata);

		Document fetched = getService(context).fetch(uid);
		assertNull(fetched);

	}

	@Test
	public void testList() throws Exception {

		// Doc 1
		InputStream is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");

		String uid = UUID.randomUUID().toString();
		Document doc = new Document();
		doc.content = ByteStreams.toByteArray(is);

		DocumentMetadata meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "wat.jpg";
		meta.mime = "image/jpg";
		meta.name = "wat ??";
		meta.description = "description wat ??";

		doc.metadata = meta;

		getService(context).create(uid, doc);

		// Doc 2
		is = DocumentServiceTests.class.getClassLoader().getResourceAsStream("data/panda.jpg");
		doc.content = ByteStreams.toByteArray(is);
		meta = new DocumentMetadata();
		meta.uid = uid;
		meta.filename = "panda.jpg";
		meta.mime = "image/jpg";
		meta.name = "pandipanda ??";
		meta.description = "description pandipanda ??";

		doc.metadata = meta;

		getService(context).create(uid, doc);

		List<DocumentMetadata> docs = getService(context).list();
		assertEquals(2, docs.size());

	}

	@Test
	public void testAcl() {
		try {
			getService(SecurityContext.ANONYMOUS).create(null, null);
			fail();
		} catch (Exception e) {

		}
		try {
			getService(SecurityContext.ANONYMOUS).update(null, null);
			fail();
		} catch (Exception e) {

		}
		try {
			getService(SecurityContext.ANONYMOUS).delete(null);
			fail();
		} catch (Exception e) {

		}
		try {
			getService(SecurityContext.ANONYMOUS).fetch(null);
			fail();
		} catch (Exception e) {

		}
		try {
			getService(SecurityContext.ANONYMOUS).fetchMetadata(null);
			fail();
		} catch (Exception e) {

		}
	}
}

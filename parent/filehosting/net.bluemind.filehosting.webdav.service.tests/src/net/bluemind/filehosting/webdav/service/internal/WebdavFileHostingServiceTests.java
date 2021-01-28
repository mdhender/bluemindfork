package net.bluemind.filehosting.webdav.service.internal;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream.AccumulatorStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class WebdavFileHostingServiceTests {
	private IFileHosting service;

	private static final String domainName = "testdomain.loc";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		final CountDownLatch latch = new CountDownLatch(1);
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				latch.countDown();
			}
		};
		VertxPlatform.spawnVerticles(done);
		latch.await();

		PopulateHelper.initGlobalVirt();

		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> values = new HashMap<>();
		values.put(DomainSettingsKeys.mail_routing_relay.name(), "runtest.loc");
		settings.set(values);

		PopulateHelper.addDomain(domainName);
		String user = "user" + System.currentTimeMillis();
		PopulateHelper.addUser(user, domainName, Mailbox.Routing.none, "canUseFilehosting", "canRemoteAttach");

		service = getService(user);

	}

	protected IFileHosting getService(String user) throws ServerFault {

		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IFileHosting.class, domainName);
	}

	@After
	public void tearDown() throws IOException {
		try {
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}
	}

	@Test
	public void testAddingAFile() throws Exception {
		String path = "te st.txt";
		String testString = "test";
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path, bytesToStream);

		String fetched = streamToString(service.get(path));

		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testCheckingExistence() throws Exception {
		String path = "test123.txt";
		String testString = "test";
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path, bytesToStream);

		assertTrue(service.exists(path));
		assertFalse(service.exists(path + System.currentTimeMillis()));
	}

	@Test
	public void testAddingAFileInASubFolder() throws Exception {
		String path = "parent/sub1/sub2/te st.txt";
		String testString = "test";
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path, bytesToStream);

		String fetched = streamToString(service.get(path));
		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testListingFiles() throws Exception {
		String parent = System.currentTimeMillis() + "";
		String path1 = parent + "/sub1/su b2/te st1.txt";
		String path2 = parent + "/sub1/su b2/te st2.txt";
		String testString = "test";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		System.err.println("listing...");
		List<FileHostingItem> list = service.list(parent + "/sub1/su b2/");

		for (FileHostingItem fileHostingItem : list) {
			System.out.println(fileHostingItem.path);
		}
		Assert.assertEquals(2, list.size());
	}

	@Test
	public void testSearch() throws Exception {
		String parent = System.currentTimeMillis() + "";
		String path1 = parent + "/sub1/sub2/tes t1.txt";
		String path2 = parent + "/sub1/sub2/tes t2.txt";
		String testString = "test";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		List<FileHostingItem> list = service.find("tes t");
		Assert.assertTrue(list.size() >= 2);

		list = service.find("no-existent");
		Assert.assertEquals(0, list.size());
	}

	@Test
	public void testDeletingAFile() throws Exception {
		String path = "tes t.txt";
		String testString = "test";
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path, bytesToStream);

		String fetched = streamToString(service.get(path));
		Assert.assertEquals(testString, fetched);

		service.delete(path);

		Assert.assertNull(service.get(path));
	}

	private Stream bytesToStream(byte[] b) throws IOException {
		return VertxStream.stream(Buffer.buffer(b));
	}

	private String streamToString(Stream stream) {
		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<Buffer> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		reader.pipeTo(writer, h -> latch.countDown());
		reader.resume();
		try {
			boolean ok = latch.await(10, TimeUnit.MINUTES);
			if (!ok) {
				throw new ServerFault("streamToString failed to complete in 1 minute");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();
	}

}

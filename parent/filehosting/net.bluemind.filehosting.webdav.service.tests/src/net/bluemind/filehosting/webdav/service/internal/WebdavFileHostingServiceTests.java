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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.core.streams.ReadStream;
import org.vertx.java.core.streams.WriteStream;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
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
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path1, bytesToStream);
		service.store(path2, bytesToStream);

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
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path1, bytesToStream);
		service.store(path2, bytesToStream);

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
		return VertxStream.stream(new Buffer(b));
	}

	private String streamToString(Stream stream) {
		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<?> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();

		reader.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				latch.countDown();
			}
		});

		Pump pump = Pump.createPump(reader, writer);
		pump.start();
		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();
	}

	private static class AccumulatorStream implements WriteStream<AccumulatorStream> {

		private Buffer buffer = new Buffer();

		@Override
		public AccumulatorStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public AccumulatorStream setWriteQueueMaxSize(int maxSize) {
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return false;
		}

		@Override
		public AccumulatorStream drainHandler(Handler<Void> handler) {
			return this;
		}

		@Override
		public AccumulatorStream write(Buffer data) {
			synchronized (this) {
				buffer.appendBuffer(data);
			}
			return this;

		}

		public Buffer buffer() {
			return buffer;
		}
	}

}

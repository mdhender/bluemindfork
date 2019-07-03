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
package net.bluemind.filehosting.filesystem.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

import com.google.common.collect.Lists;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.ID;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.filesystem.service.internal.persistence.FileHostingEntityInfo;
import net.bluemind.filehosting.filesystem.service.internal.persistence.FileHostingStore;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class FileSystemFileHostingServiceTests {
	private IFileHosting service;

	private static final String domainName = "testdomain.loc";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		

		final CountDownLatch latch = new CountDownLatch(1);
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				latch.countDown();
			}
		};
		VertxPlatform.spawnVerticles(done);
		latch.await();

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList("filehosting/data");

		PopulateHelper.initGlobalVirt(nodeServer);

		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> values = new HashMap<>();
		values.put(DomainSettingsKeys.mail_routing_relay.name(), "runtest.loc");
		settings.set(values);

		PopulateHelper.addDomain(domainName);
		PopulateHelper.addUser("user", domainName, Mailbox.Routing.none, "canUseFilehosting", "canRemoteAttach");

		service = getService();

		deleteAllFiles(FileSystemFileHostingService.DEFAULT_STORE_PATH);

	}

	private void deleteAllFiles(String filepath) throws ServerFault {
		List<FileDescription> listFiles = getNodeClient().listFiles(filepath);
		for (FileDescription fileDescription : listFiles) {
			if (fileDescription.isDirectory()) {
				deleteAllFiles(fileDescription.getPath());
			} else {
				getNodeClient().deleteFile(fileDescription.getPath());
			}
		}
	}

	private INodeClient getNodeClient() throws ServerFault {
		LocatorClient lc = new LocatorClient();
		String ip = lc.locateHost("filehosting/data", "admin0@global.virt");
		return NodeActivator.get(ip);
	}

	protected IFileHosting getService() throws ServerFault {

		IAuthentication auth = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);

		LoginResponse login = auth.login("user@" + domainName, "user", null);

		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", login.authKey)
				.instance(IFileHosting.class, domainName);
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
		String path = "/test.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));

		String fetched = streamToString(service.get(path));

		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testAddingAFileShouldWriteInfoData() throws Exception {
		String path = "/test.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));

		FileHostingStore store = new FileHostingStore(JdbcActivator.getInstance().getDataSource());
		List<FileHostingEntityInfo> expiredFiles = store.getExpiredFiles(-1);

		Assert.assertEquals(1, expiredFiles.size());
		Assert.assertTrue(expiredFiles.get(0).path.startsWith(domainName));
		Assert.assertTrue(expiredFiles.get(0).path.endsWith(path));
	}

	@Test
	public void testCleaningUpExpiredFiles() throws Exception {
		String path = "/testCleaningUpExpiredFiles.txt";
		String path2 = "/testCleaningUpExpiredFiles2.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		FileHostingStore store = new FileHostingStore(JdbcActivator.getInstance().getDataSource());
		List<FileHostingEntityInfo> expiredFiles = store.getExpiredFiles(-1);

		Assert.assertEquals(2, expiredFiles.size());

		String fetched = streamToString(service.get(path));
		Assert.assertEquals(testString, fetched);
		fetched = streamToString(service.get(path2));
		Assert.assertEquals(testString, fetched);

		// need to lookup the service directly to be able to access non-API
		// methods
		FileSystemFileHostingService fileSystemFileHostingService = (FileSystemFileHostingService) lookupExtensionPoint();
		int cleanupFiles = fileSystemFileHostingService.cleanup(-1, domainName);

		Assert.assertEquals(2, cleanupFiles);
		try {
			streamToString(service.get(path));
			streamToString(service.get(path2));
			Assert.fail();
		} catch (Exception e) {

		}
		Assert.assertEquals(0, store.getExpiredFiles(-1).size());

		service.store(path, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_YEAR, 10);

		// share will prevent the job from deleting the file
		FileHostingPublicLink publicUrl = service.share(path2, -1,
				BmDateTimeWrapper.toIso8601(cal.getTimeInMillis(), "UTC"));

		Assert.assertEquals(cal.getTimeInMillis(), publicUrl.expirationDate.longValue());

		cleanupFiles = fileSystemFileHostingService.cleanup(-1, domainName);

		Assert.assertEquals(1, cleanupFiles);
		try {
			streamToString(service.get(path));
			Assert.fail();
		} catch (Exception e) {

		}
		streamToString(service.get(path2));
	}

	private IFileHostingService lookupExtensionPoint() {
		RunnableExtensionLoader<IFileHostingService> epLoader = new RunnableExtensionLoader<>();
		List<IFileHostingService> extensions = epLoader.loadExtensions("net.bluemind.filehosting", "service", "service",
				"api");
		return extensions.isEmpty() ? null : extensions.get(0);
	}

	@Test
	public void testCleaningUpExpiredFilesRetentionTime() throws Exception {
		String path = "/test.txt";
		String path2 = "/test2.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		FileHostingStore store = new FileHostingStore(JdbcActivator.getInstance().getDataSource());
		List<FileHostingEntityInfo> expiredFiles = store.getExpiredFiles(1);

		Assert.assertEquals(0, expiredFiles.size());
	}

	@Test
	public void testAddingAFileInSubFolder() throws Exception {
		String path = "/test/test.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));
		System.err.println("stored..");
		String fetched = streamToString(service.get(path));

		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testGettingAFile() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		String fetched = streamToString(service.get(path));

		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testGettingABigFile() throws Exception {
		String testString = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			sb.append(testString);
		}
		String target = sb.toString();
		String path = "/test.txt";
		long time = System.currentTimeMillis();
		service.store(path, bytesToStream(target.getBytes()));
		System.out.println("took " + (System.currentTimeMillis() - time) + " ms to write "
				+ (10000 * testString.length()) + " bytes");

		time = System.currentTimeMillis();
		String fetched = streamToString(service.get(path));
		System.out.println("took " + (System.currentTimeMillis() - time) + " ms to read "
				+ (10000 * testString.length()) + " bytes");

		Assert.assertEquals(target, fetched);
	}

	@Test
	public void testTimeOut() throws Exception {
		String path = "/test.txt";
		service.store(path, bytesToTimeoutStream(40));
	}

	@Test
	public void testFileMaxDataSize() throws Exception {
		String testString = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb.append(testString);
		}
		String target = sb.toString();
		byte[] data = target.getBytes();

		IGlobalSettings systemConfig = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> values = new HashMap<String, String>();
		values.put(GlobalSettingsKeys.filehosting_max_filesize.name(), String.valueOf((data.length / 2)));
		systemConfig.set(values);

		String path = "/test.txt";
		try {
			service.store(path, bytesToStream(data));
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("The filesize exceeds the maximum"));
		}
	}

	@Test
	public void testManyFiles() throws Exception {
		for (int i = 0; i < 100; i++) {
			System.out.println("current " + i);
			testGettingAFile();
		}
	}

	@Test
	public void testGettingAFileShouldReturnRelativePathInMetadata() throws Exception {
		String testString = "test";
		String path = "/sub/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		List<FileHostingItem> fetched = service.find("test.txt");

		Assert.assertEquals(path, fetched.get(0).path);
	}

	@Test
	public void testSharingAFile() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		FileHostingPublicLink publicLink = service.share(path, -1, null);
		String id = ID.extract(publicLink.url);

		FileHostingItem complete = service.getComplete(id);
		service.getSharedFile(id);
		Assert.assertEquals("test.txt", complete.name);
	}

	@Test
	public void testGettingASharedFileShouldCheckDownloadLimit() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		FileHostingPublicLink publicLink = service.share(path, 3, null);

		String id = ID.extract(publicLink.url);

		service.getSharedFile(id);
		service.getSharedFile(id);
		service.getSharedFile(id);

		try {
			service.getSharedFile(id);
			Assert.fail();
		} catch (ServerFault s) {

		}
	}

	@Test
	public void testGettingASharedFileShouldCheckExpirationDate() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		FileHostingPublicLink publicLink = service.share(path, -1, "2015-05-28T16:45:43.355Z");
		String id = ID.extract(publicLink.url);

		try {
			service.getSharedFile(id);
			Assert.fail();
		} catch (ServerFault s) {

		}

	}

	@Test
	public void testGettingASharedFileWithExpirationDate() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		String expirationDate = "2025-05-28T16:45:43.355Z";
		FileHostingPublicLink publicLink = service.share(path, -1, expirationDate);
		String id = ID.extract(publicLink.url);

		assertNotNull(publicLink.expirationDate);
		assertEquals(expirationDate, new DateTime(publicLink.expirationDate, DateTimeZone.UTC).toString());

		service.getSharedFile(id);
	}

	@Test
	public void testListingADirectory() throws Exception {
		String testString = "test";
		String path1 = "/sub/sub/test.txt";
		String path2 = "/sub/sub/test2.txt";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		List<FileHostingItem> listing = service.list("/sub/sub");

		Assert.assertEquals(2, listing.size());
	}

	@Test
	public void testSearchingAnItem() throws Exception {
		String testString = "test";
		String path1 = "/sub/sub/test.txt";
		String path2 = "/sub/sub/test2.txt";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		List<FileHostingItem> hits = service.find("test.txt");

		Assert.assertEquals(1, hits.size());
		Assert.assertEquals("test.txt", hits.get(0).name);
	}

	@Test
	public void testSearchingAnItemShouldUseIndexOfSearch() throws Exception {
		String testString = "test";
		String path1 = "/sub/sub/test.txt";
		String path2 = "/sub/sub/test2.txt";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		List<FileHostingItem> hits = service.find("test");

		Assert.assertEquals(2, hits.size());
	}

	@Test
	public void testAddingAnExistingFileShouldOverwriteExistingFile() throws Exception {
		String testString1 = "test";
		String testString2 = "updated";
		String path = "/sub/sub/test.txt";

		service.store(path, bytesToStream(testString1.getBytes()));
		String fetched1 = streamToString(service.get(path));

		service.store(path, bytesToStream(testString2.getBytes()));
		String fetched2 = streamToString(service.get(path));

		Assert.assertEquals(testString1, fetched1);
		Assert.assertEquals(testString2, fetched2);
	}

	@Test
	public void testAddingAFileShouldCreateMissingPath() throws Exception {
		String testString = "test";
		String path = "/sub/sub/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		String fetched = streamToString(service.get(path));

		Assert.assertEquals(testString, fetched);
	}

	@Test
	public void testGettingNonExistingFileShouldThrowException() {
		String iDontExist = "/sub/sub/test.txt";

		try {
			service.get(iDontExist);
		} catch (ServerFault e) {
			return;
		}

		Assert.fail();
	}

	@Test
	public void testSharingNonExistingFileShouldThrowException() {
		String iDontExist = "/sub/sub/test-idontexists.txt";
		// FIXME bad pattern
		// try { doSomethingShouldFail(); fail(); }
		// catch(ExpectedException e){}
		try {
			service.share(iDontExist, -1, null);
		} catch (ServerFault e) {
			return;
		}

		Assert.fail();
	}

	@Test
	public void testListingNonExistingDirectoryShouldReturnEmptyList() throws Exception {
		String iDontExist = "/sub/sub/test.txt";

		List<FileHostingItem> listing = service.list(iDontExist);

		Assert.assertEquals(0, listing.size());
	}

	@Test
	public void testListingFileInsteadOfFolderShouldIgnoreFilename() throws Exception {
		String testString = "test";
		String path = "/sub/sub/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		List<FileHostingItem> listing = service.list(path);

		Assert.assertEquals(1, listing.size());
	}

	@Test
	public void testSearchingShouldBeCaseInsensitive() throws Exception {
		String testString = "test";
		String path1 = "/sub/sub/test.txt";
		String path2 = "/sub/sub/test2.txt";
		service.store(path1, bytesToStream(testString.getBytes()));
		service.store(path2, bytesToStream(testString.getBytes()));

		List<FileHostingItem> hits = service.find("TeSt");

		Assert.assertEquals(2, hits.size());
	}

	@Test
	public void testSearchingNonExistingFileShouldReturnEmptyList() throws Exception {
		List<FileHostingItem> hits = service.find("test");

		Assert.assertEquals(0, hits.size());

	}

	@Test
	public void testGettingConfiguration() throws Exception {
		Configuration configuration = service.getConfiguration();

		Assert.assertNotNull(configuration);
	}

	private Stream bytesToStream(byte[] b) throws IOException {
		return VertxStream.stream(new Buffer(b));
	}

	private Stream bytesToTimeoutStream(int secs) throws IOException {
		TimeoutReadStream stream = new TimeoutReadStream(secs);
		return VertxStream.stream(stream);
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

	private static class TimeoutReadStream implements ReadStream<TimeoutReadStream> {

		private Handler<Buffer> dh;
		private Handler<Void> eh;
		private boolean sent = false;
		private AtomicBoolean paused = new AtomicBoolean(false);
		private int secs;

		public TimeoutReadStream(int secs) {
			this.secs = secs;
		}

		@Override
		public TimeoutReadStream dataHandler(Handler<Buffer> handler) {
			this.dh = handler;
			new Thread(() -> {
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
				int totalWrote = 0;
				long until = System.currentTimeMillis() + (this.secs * 1000);

				String chunk = StringUtils.leftPad("", 128, 'x');

				while (System.currentTimeMillis() < until) {
					if (!paused.get()) {
						totalWrote++;
						dh.handle(new Buffer(chunk));
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
				System.out.println("wrote " + totalWrote + "bytes... in " + secs + " seconds");
				sent = true;
				if (null != eh) {
					eh.handle(null);
				}
			}).start();
			return this;
		}

		@Override
		public TimeoutReadStream pause() {
			paused.set(true);
			return this;
		}

		@Override
		public TimeoutReadStream resume() {
			paused.set(false);
			return this;
		}

		@Override
		public TimeoutReadStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public TimeoutReadStream endHandler(Handler<Void> endHandler) {
			this.eh = endHandler;
			if (sent) {
				eh.handle(null);
			}
			return this;
		}

	}

}

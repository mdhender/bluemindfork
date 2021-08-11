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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.base.GenericStream.AccumulatorStream;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
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
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class FileSystemFileHostingServiceTests {
	private IFileHosting service;
	IDomainSettings domainSettings;
	ISystemConfiguration systemConfiguration;

	private static final String DOMAIN_NAME = "testdomain.loc";
	private static final String DOMAIN_EXTERNAL_URL = "my.test.domain.external.url";
	private static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(1, TimeUnit.MINUTES);

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp("bluemind/node-tests");
		nodeServer.tags = Lists.newArrayList("filehosting/data");

		Server imapServer = new Server();
		imapServer.ip = DockerEnv.getIp("bluemind/imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(nodeServer, imapServer);

		IGlobalSettings settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> values = new HashMap<>();
		values.put(DomainSettingsKeys.mail_routing_relay.name(), "runtest.loc");
		settings.set(values);

		PopulateHelper.addDomain(DOMAIN_NAME);
		PopulateHelper.addUser("user", DOMAIN_NAME, Mailbox.Routing.none, "canUseFilehosting", "canRemoteAttach");

		systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);

		domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class,
				DOMAIN_NAME);
		Map<String, String> domainValues = new HashMap<>();
		domainValues.put(DomainSettingsKeys.external_url.name(), DOMAIN_EXTERNAL_URL);
		domainSettings.set(domainValues);

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

	private INodeClient getNodeClient() {
		String ip = Topology.get().any("filehosting/data").value.address();
		return NodeActivator.get(ip);
	}

	protected IFileHosting getService() throws ServerFault {

		IAuthentication auth = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);

		LoginResponse login = auth.login("user@" + DOMAIN_NAME, "user", null);

		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", login.authKey)
				.instance(IFileHosting.class, DOMAIN_NAME);
	}

	@After
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
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
	public void testCheckingExistence() throws Exception {
		String path = "/test123.txt";
		String testString = "test";
		Stream bytesToStream = bytesToStream(testString.getBytes());
		service.store(path, bytesToStream);

		assertTrue(service.exists(path));
		assertFalse(service.exists(path + System.currentTimeMillis()));
	}

	@Test
	public void testAddingAFileShouldWriteInfoData() throws Exception {
		String path = "/test.txt";
		String testString = "test";
		service.store(path, bytesToStream(testString.getBytes()));

		FileHostingStore store = new FileHostingStore(JdbcActivator.getInstance().getDataSource());
		List<FileHostingEntityInfo> expiredFiles = store.getExpiredFiles(-1);

		Assert.assertEquals(1, expiredFiles.size());
		Assert.assertTrue(expiredFiles.get(0).path.startsWith(DOMAIN_NAME));
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
		int cleanupFiles = fileSystemFileHostingService.cleanup(-1, DOMAIN_NAME);

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

		cleanupFiles = fileSystemFileHostingService.cleanup(-1, DOMAIN_NAME);

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
		System.err.println("gettingABigFile starts....");
		Path tmpStuff = Files.createTempFile("yeah", ".bin");
		tmpStuff.toFile().deleteOnExit();
		Random rd = ThreadLocalRandom.current();
		byte[] holder = new byte[1024 * 1024];
		for (int i = 0; i < 100; i++) {
			rd.nextBytes(holder);
			Files.write(tmpStuff, holder, StandardOpenOption.APPEND);
		}
		AsyncFile stream = VertxPlatform.getVertx().fileSystem().openBlocking(tmpStuff.toFile().getAbsolutePath(),
				new OpenOptions());
		String path = "/test.txt";
		long time = System.currentTimeMillis();
		service.store(path, VertxStream.stream(stream));
		System.out.println("took " + (System.currentTimeMillis() - time) + " ms to write " + tmpStuff.toFile().length()
				+ " bytes");
		System.err.println("Getting file " + path);
		Stream retreived = service.get(path);
		System.err.println("Got " + retreived);
		CompletableFuture<Buffer> futBuf = GenericStream.asyncStreamToBuffer(retreived);
		futBuf.whenComplete((v, ex) -> {
			assertNull(ex);
			assertEquals(tmpStuff.toFile().length(), v.length());
			try {
				Files.delete(tmpStuff);
			} catch (IOException e) {
			}
		}).get(40, TimeUnit.SECONDS);
		assertFalse(futBuf.isCompletedExceptionally());
	}

	@Test
	public void testSlowGet() throws Exception {
		System.err.println("testSlowGet starts....");
		Path tmpStuff = Files.createTempFile("yeah", ".bin");
		tmpStuff.toFile().deleteOnExit();
		Random rd = ThreadLocalRandom.current();
		byte[] holder = new byte[1024];
		for (int i = 0; i < 100; i++) {
			rd.nextBytes(holder);
			Files.write(tmpStuff, holder, StandardOpenOption.APPEND);
		}
		AsyncFile stream = VertxPlatform.getVertx().fileSystem().openBlocking(tmpStuff.toFile().getAbsolutePath(),
				new OpenOptions());
		String path = "/test.txt";
		long time = System.currentTimeMillis();
		service.store(path, VertxStream.stream(stream));
		System.out.println("took " + (System.currentTimeMillis() - time) + " ms to write " + tmpStuff.toFile().length()
				+ " bytes");
		System.err.println("Getting file " + path);
		Stream retreived = service.get(path);
		System.err.println(new Date() + " Got " + retreived);
		CompletableFuture<Void> futBuf = GenericStream.slowRead(retreived);
		futBuf.whenComplete((v, ex) -> {
			try {
				Files.delete(tmpStuff);
			} catch (IOException e) {
			}
		}).get(2, TimeUnit.MINUTES);
	}

	@Test
	public void testTimeOut() throws Exception {
		String path = "/test.txt";
		service.store(path, bytesToTimeoutStream(20));
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
			System.err.println("msg " + e.getMessage() + " class " + e.getClass());
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
	public void testSharingAFile_withDomainExternalUrl() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		String domainExternalUrl = domainSettings.get().get(DomainSettingsKeys.external_url.name());
		assertEquals(DOMAIN_EXTERNAL_URL, domainExternalUrl);

		FileHostingPublicLink publicLink = service.share(path, -1, null);
		assertTrue(publicLink.url.contains("://" + domainExternalUrl));
		String id = ID.extract(publicLink.url);

		FileHostingItem complete = service.getComplete(id);
		service.getSharedFile(id);
		Assert.assertEquals("test.txt", complete.name);
	}

	@Test
	public void testSharingAFile_withGlobalExternalUrl() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		domainSettings.set(new HashMap<String, String>());
		assertNull(domainSettings.get().get(DomainSettingsKeys.external_url.name()));

		String externalUrl = systemConfiguration.getValues().values.get(SysConfKeys.external_url.name());
		assertEquals(GLOBAL_EXTERNAL_URL, externalUrl);

		FileHostingPublicLink publicLink = service.share(path, -1, null);
		assertTrue(publicLink.url.contains("://" + externalUrl));
		String id = ID.extract(publicLink.url);

		FileHostingItem complete = service.getComplete(id);
		service.getSharedFile(id);
		Assert.assertEquals("test.txt", complete.name);
	}

	@Test
	public void testSharingAFile_withoutAnyExternalUrl() throws Exception {
		String testString = "test";
		String path = "/test.txt";
		service.store(path, bytesToStream(testString.getBytes()));

		domainSettings.set(new HashMap<String, String>());
		assertNull(domainSettings.get().get(DomainSettingsKeys.external_url.name()));

		Map<String, String> sysconfValues = systemConfiguration.getValues().values;
		sysconfValues.put(SysConfKeys.external_url.name(), null);
		systemConfiguration.updateMutableValues(sysconfValues);
		assertNull(systemConfiguration.getValues().values.get(SysConfKeys.external_url.name()));

		FileHostingPublicLink publicLink = service.share(path, -1, null);
		assertTrue(publicLink.url.contains("://configure.your.external.url"));
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

		ZonedDateTime date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(publicLink.expirationDate), ZoneId.of("UTC"));
		assertEquals(expirationDate + "[UTC]", date.toString());

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
		return VertxStream.stream(Buffer.buffer(b));
	}

	private Stream bytesToTimeoutStream(int secs) throws IOException {
		TimeoutReadStream stream = new TimeoutReadStream(secs);
		return VertxStream.stream(stream);
	}

	private String streamToString(Stream stream) {
		final CountDownLatch latch = new CountDownLatch(1);
		final ReadStream<Buffer> reader = VertxStream.read(stream);
		final AccumulatorStream writer = new AccumulatorStream();
		reader.pipeTo(writer, h -> latch.countDown());
		reader.resume();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return writer.buffer().toString();
	}

	private static class TimeoutReadStream implements ReadStream<Buffer> {

		private Handler<Buffer> dh;
		private Handler<Void> eh;
		private boolean sent = false;
		private AtomicBoolean paused = new AtomicBoolean(false);
		private int secs;

		public TimeoutReadStream(int secs) {
			this.secs = secs;
		}

		@Override
		public TimeoutReadStream handler(Handler<Buffer> handler) {
			this.dh = handler;
			new Thread(() -> {
				try {
					Thread.sleep(150);
				} catch (InterruptedException e) {
				}
				int totalWrote = 0;
				long until = System.currentTimeMillis() + (this.secs * 1000);

				String chunk = Strings.padStart("", 128, 'x');

				while (System.currentTimeMillis() < until) {
					if (!paused.get()) {
						totalWrote++;
						dh.handle(Buffer.buffer(chunk));
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

		@Override
		public ReadStream<Buffer> fetch(long amount) {
			return this;
		}

	}

}

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
package net.bluemind.attachment.service.internal;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.ID;
import net.bluemind.filehosting.api.IInternalBMFileSystem;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AttachmentServiceTests {
	private IAttachment service;
	private File rootFolder;
	private File tmpFolder;
	private SecurityContext securityContext;

	protected static final String domainName = "testdomain.loc";
	private static final String GLOBAL_EXTERNAL_URL = "my.test.external.url";

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server nodeServer = new Server();
		nodeServer.ip = DockerEnv.getIp(DockerContainer.NODE.getName());
		nodeServer.tags = Lists.newArrayList(TagDescriptor.bm_filehosting.getTag());

		PopulateHelper.initGlobalVirt(esServer, nodeServer);

		String subject = "" + System.currentTimeMillis();
		securityContext = new SecurityContext(null, subject, Arrays.asList(),
				Arrays.asList(SecurityContext.ROLE_SYSTEM), Collections.emptyMap(), "global.virt", "en",
				"internal-system");

		ISystemConfiguration systemConfiguration = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class);
		Map<String, String> sysValues = systemConfiguration.getValues().values;
		sysValues.put(SysConfKeys.external_url.name(), GLOBAL_EXTERNAL_URL);
		systemConfiguration.updateMutableValues(sysValues);

		service = getAttachmentService(securityContext);
		this.rootFolder = new File("/var/spool/bm-filehosting");
		try {
			rootFolder.mkdirs();
		} catch (Exception e) {

		}
		if (!rootFolder.exists()) {
			this.rootFolder = new File(System.getProperty("java.io.tmpdir"), "bm-filehosting");
		}
		this.tmpFolder = new File(System.getProperty("java.io.tmpdir"), "bm-filehosting-tmp");
		FileUtils.deleteQuietly(tmpFolder);
		this.tmpFolder.mkdirs();
		FileUtils.deleteQuietly(rootFolder);
	}

	protected IInternalBMFileSystem getFileHostingService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IInternalBMFileSystem.class);
	}

	protected IAttachment getAttachmentService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(securityContext).instance(IAttachment.class, domainName);
	}

	@After
	public void tearDown() throws IOException {
		try {
			JdbcTestHelper.getInstance().afterTest();
		} catch (Exception e) {
		}
	}

	@Test
	public void testDetachingAFile() throws Exception {
		String name = "test.txt";
		String testString = "test";

		AttachedFile file = service.share(name, bytesToStream(testString.getBytes()));

		Assert.assertEquals(name, file.name);
		Assert.assertNotNull(file.publicUrl);

		Assert.assertTrue(fileExists(file.name));
	}

	@Test
	public void testDetachingDedup() throws Exception {
		String testString = "test";

		AttachedFile file = service.shareDedup("txt", bytesToStream(testString.getBytes()));
		AttachedFile another = service.shareDedup("txt", bytesToStream(testString.getBytes()));
		System.err.println("file " + file.publicUrl + " " + file.name);
		System.err.println("anot " + another.publicUrl + " " + another.name);

		Assert.assertNotNull(file.publicUrl);
		Assert.assertNotEquals(file.publicUrl, another.publicUrl);
		Assert.assertEquals(file.name, another.name);
		Assert.assertTrue(fileExists(file.name));
	}

	@Test
	public void testDetachingMultipleFilesWithSameFilename() throws Exception {
		String name = "test.txt";
		String testString = "test";

		AttachedFile file1 = service.share(name, bytesToStream(testString.getBytes()));
		AttachedFile file2 = service.share(name, bytesToStream(testString.getBytes()));
		AttachedFile file3 = service.share(name, bytesToStream(testString.getBytes()));

		Assert.assertEquals(name, file1.name);
		Assert.assertNotNull(file1.publicUrl);

		Assert.assertEquals(name, file2.name);
		Assert.assertNotNull(file2.publicUrl);

		Assert.assertEquals(name, file3.name);
		Assert.assertNotNull(file3.publicUrl);

		Assert.assertNotEquals(file1.publicUrl, file2.publicUrl);
		Assert.assertNotEquals(file1.publicUrl, file3.publicUrl);
		Assert.assertNotEquals(file2.publicUrl, file3.publicUrl);
	}

	@Test
	public void testDetachingMultipleFilesWithSameFilenameWithoutExtension() throws Exception {
		String name = "test";
		String testString = "test";

		AttachedFile file1 = service.share(name, bytesToStream(testString.getBytes()));
		AttachedFile file2 = service.share(name, bytesToStream(testString.getBytes()));
		AttachedFile file3 = service.share(name, bytesToStream(testString.getBytes()));

		Assert.assertEquals(name, file1.name);
		Assert.assertNotNull(file1.publicUrl);

		Assert.assertEquals(name, file2.name);
		Assert.assertNotNull(file2.publicUrl);

		Assert.assertEquals(name, file3.name);
		Assert.assertNotNull(file3.publicUrl);

		Assert.assertNotEquals(file1.publicUrl, file2.publicUrl);
		Assert.assertNotEquals(file1.publicUrl, file3.publicUrl);
		Assert.assertNotEquals(file2.publicUrl, file3.publicUrl);
	}

	@Test
	public void testUnsharingAFileShouldDeleteFileInfoEntity() throws Exception {
		String name = "test.txt";
		String testString = "test";

		AttachedFile file = service.share(name, bytesToStream(testString.getBytes()));

		FileHostingItem fileInfo = getFileHostingService(securityContext).getComplete(ID.extract(file.publicUrl));

		Assert.assertNotNull(fileInfo);
		Assert.assertEquals("test.txt", fileInfo.name);

		service.unShare(file.publicUrl);

		try {
			fileInfo = getFileHostingService(securityContext).getComplete(ID.extract(file.publicUrl));
			Assert.fail();
		} catch (Exception e) {
		}

	}

	@Test
	public void testUnsharingAFileShouldNotDeleteFile() throws Exception {
		String name = "test.txt";
		String testString = "test";

		AttachedFile file = service.share(name, bytesToStream(testString.getBytes()));

		FileHostingItem fileInfo = getFileHostingService(securityContext).getComplete(ID.extract(file.publicUrl));

		Assert.assertNotNull(fileInfo);
		Assert.assertEquals("test.txt", fileInfo.name);

		service.unShare(file.publicUrl);

		assertTrue(fileExists(fileInfo.path));

	}

	private boolean fileExists(String path) throws ServerFault {
		return (getFileHostingService(securityContext) //
				.list(AttachmentService.FOLDER) //
				.stream() //
				.filter(item -> item.name.equals("")) //
				.count()) == 0;
	}

	private Stream bytesToStream(byte[] b) throws IOException {
		SimpleReadStream stream = new SimpleReadStream(new String(b));
		return VertxStream.stream(stream);
	}

	private static class SimpleReadStream implements ReadStream<Buffer> {

		private Handler<Buffer> dh;
		private Handler<Void> eh;
		private Handler<Throwable> ex;
		private String data;
		private boolean sent = false;

		public SimpleReadStream(String data) {
			this.data = data;
		}

		@Override
		public SimpleReadStream handler(Handler<Buffer> handler) {
			this.dh = handler;
			dh.handle(Buffer.buffer(data));
			sent = true;
			if (null != eh) {
				eh.handle(null);
			}
			return this;
		}

		@Override
		public SimpleReadStream pause() {
			return this;
		}

		@Override
		public SimpleReadStream resume() {
			return this;
		}

		@Override
		public SimpleReadStream exceptionHandler(Handler<Throwable> handler) {
			this.ex = handler;
			return this;
		}

		@Override
		public SimpleReadStream endHandler(Handler<Void> endHandler) {
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

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.serialization.client.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.hollow.datamodel.consumer.DirectoryDeserializer;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.directory.hollow.datamodel.consumer.SerializedDirectorySearch;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.serialization.client.BmHollowClient;
import net.bluemind.serialization.client.BmHollowClient.Type;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class BmHollowClientTests {

	@Before
	public void before() throws Exception {
		cleanupHollowData();
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		PopulateHelper.addDomain("bm.lan", Routing.none);
		PopulateHelper.addUser("john", "bm.lan");

		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.until(() -> new File(DirectoryDeserializer.baseDataDir() + "/bm.lan/announced.version").exists());

		SerializedDirectorySearch hollow = DirectorySearchFactory.get("bm.lan");

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !hollow.all().isEmpty());

	}

	@After
	public void after() throws Exception {
		cleanupHollowData();
		JdbcTestHelper.getInstance().afterTest();
	}

	private void cleanupHollowData() {
		System.err.println("cleanup of hollow data");
		DirectorySearchFactory.reset();

		File dir = new File("/var/spool/bm-hollowed/directory");
		if (dir.exists() && dir.isDirectory()) {
			try {
				Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder())
						.map(Path::toFile).forEach(File::delete);
			} catch (Exception e) {
				e.printStackTrace();
			}
			dir.mkdirs();
		}
	}

	@Test
	public void downloadWithClient() throws IOException {
		long version;
		try (BmHollowClient bhc = new BmHollowClient(Type.version, "directory", "bm.lan", 0)) {
			version = bhc.getVersion();
			System.err.println("version: " + version);
		}
		assertTrue(version > 0);
		try (BmHollowClient bhc = new BmHollowClient(Type.snapshot, "directory", "bm.lan", version);
				InputStream stream = bhc.openStream()) {
			byte[] copy = ByteStreams.toByteArray(stream);
			assertNotNull(copy);
			assertTrue(copy.length > 8192);
		}
	}

}

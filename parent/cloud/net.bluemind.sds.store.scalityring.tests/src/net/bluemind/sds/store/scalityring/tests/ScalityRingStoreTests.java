/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.store.scalityring.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.scalityring.ScalityConfiguration;
import net.bluemind.sds.store.scalityring.ScalityRingStoreFactory;
import net.bluemind.vertx.testhelper.Deploy;

public class ScalityRingStoreTests {
	public static int LENGTH = 64;
	private Set<String> vertids;

	@Before
	public void launchServer() throws Exception {
		vertids = Deploy.verticles(false, ScalityTestServer::new).get(5, TimeUnit.SECONDS);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void stopServer() {
		Deploy.afterTest(vertids);
	}

	private ISdsBackingStore getStore() {
		ScalityConfiguration config = new ScalityConfiguration("http://localhost:4552/sproxy/");
		JsonObject configjs = config.asJson();
		return new ScalityRingStoreFactory().create(VertxPlatform.getVertx(), configjs);
	}

	@Test
	public void createStore() {
		ISdsBackingStore store = getStore();
		assertNotNull(store);
	}

	@Test
	public void objectDoesNotExist() {
		ISdsBackingStore store = getStore();
		ExistRequest er = new ExistRequest();
		er.mailbox = "titi";
		er.guid = UUID.randomUUID().toString();
		ExistResponse existsResp = store.exists(er).join();
		assertFalse(existsResp.exists);
	}

	@Test
	public void putObject() throws IOException {
		ISdsBackingStore store = getStore();
		ExistRequest er = new ExistRequest();
		er.mailbox = "titi";
		er.guid = UUID.randomUUID().toString();
		ExistResponse existsResp = store.exists(er).join();
		assertFalse(existsResp.exists);

		PutRequest pr = new PutRequest();
		pr.mailbox = er.mailbox;
		pr.guid = er.guid;

		Path path = tempContent();
		pr.filename = path.toFile().getAbsolutePath();
		SdsResponse resp = store.upload(pr).join();
		assertNotNull(resp);

		assertTrue(store.exists(er).join().exists);
		Files.deleteIfExists(path);
	}

	@Test
	public void putObjectAlreadyExisting() throws IOException {
		ISdsBackingStore store = getStore();
		ExistRequest er = new ExistRequest();
		er.mailbox = "titi";
		er.guid = UUID.randomUUID().toString();
		ExistResponse existsResp = store.exists(er).join();
		assertFalse(existsResp.exists);

		PutRequest pr = new PutRequest();
		pr.mailbox = er.mailbox;
		pr.guid = er.guid;

		Path path = tempContent();
		pr.filename = path.toFile().getAbsolutePath();
		SdsResponse resp = store.upload(pr).join();
		assertNotNull(resp);

		assertTrue(store.exists(er).join().exists);
		Files.deleteIfExists(path);

		pr = new PutRequest();
		pr.mailbox = er.mailbox;
		pr.guid = er.guid;

		path = tempContent();
		pr.filename = path.toFile().getAbsolutePath();
		resp = store.upload(pr).join();
		assertNotNull(resp);
		assertTrue(store.exists(er).join().exists);
		Files.deleteIfExists(path);
	}

	@Test
	public void deleteObject() throws IOException {
		ISdsBackingStore store = getStore();

		PutRequest pr = new PutRequest();
		pr.mailbox = "tata";
		pr.guid = UUID.randomUUID().toString();
		Path path = tempContent();
		pr.filename = path.toFile().getAbsolutePath();
		SdsResponse resp = store.upload(pr).join();
		assertNull(resp.error);
		System.err.println("put ok");

		ExistRequest er = new ExistRequest();
		er.guid = pr.guid;
		er.mailbox = pr.mailbox;
		assertTrue(store.exists(er).join().exists);
		System.err.println("exists !");

		DeleteRequest dr = new DeleteRequest();
		dr.guid = pr.guid;
		dr.mailbox = pr.mailbox;
		System.err.println("deleting...");
		assertNull(store.delete(dr).join().error);

		System.err.println("exist check...");
		ExistRequest er2 = new ExistRequest();
		er2.guid = dr.guid;
		er2.mailbox = dr.mailbox;
		assertFalse(store.exists(er2).join().exists);
		System.err.println("does not exist, ok.");
		Files.deleteIfExists(path);
	}

	@Test
	public void deleteFails() throws IOException {
		ScalityConfiguration config = new ScalityConfiguration("http://localhost:1/sproxy/");
		JsonObject configjs = config.asJson();
		ISdsBackingStore store = new ScalityRingStoreFactory().create(VertxPlatform.getVertx(), configjs);

		DeleteRequest dr = new DeleteRequest();
		dr.guid = "non.existent.delete";
		dr.mailbox = "no.mailbox";
		System.err.println("deleting...");
		SdsResponse resp = store.delete(dr).join();
		System.err.println("Delete response: " + resp + "  " + resp.error);
		assertNotNull(resp.error);
	}

	@Test
	public void getObject() throws IOException {
		ISdsBackingStore store = getStore();

		PutRequest put = new PutRequest();
		put.mailbox = "titi";
		put.guid = UUID.randomUUID().toString();
		Path path = tempContent();
		put.filename = path.toFile().getAbsolutePath();
		assertNull(store.upload(put).join().error);

		GetRequest get = new GetRequest();
		get.mailbox = put.mailbox;
		get.guid = put.guid;
		get.filename = put.filename + ".download";
		SdsResponse dlResp = store.download(get).join();
		assertNotNull(dlResp);
		assertNull(dlResp.error);
		File downloaded = new File(get.filename);
		assertTrue(downloaded.exists());
		assertEquals(LENGTH, downloaded.length());
		Files.deleteIfExists(downloaded.toPath());
		Files.deleteIfExists(path);
	}

	@Test
	public void getObjects() throws IOException {
		ISdsBackingStore store = getStore();

		PutRequest put = new PutRequest();
		put.mailbox = "titi";
		put.guid = UUID.randomUUID().toString();
		Path path = tempContent();
		put.filename = path.toFile().getAbsolutePath();
		assertNull(store.upload(put).join().error);

		PutRequest put2 = new PutRequest();
		put2.mailbox = "titi";
		put2.guid = UUID.randomUUID().toString();
		Path path2 = tempContent();
		put2.filename = path2.toFile().getAbsolutePath();
		assertNull(store.upload(put2).join().error);

		MgetRequest get = new MgetRequest();
		get.mailbox = put2.mailbox;
		get.transfers = Arrays.asList(Transfer.of(put.guid, put.filename + ".dl"),
				Transfer.of(put2.guid, put2.filename + ".dl"));
		SdsResponse dlResp = store.downloads(get).join();
		assertNotNull(dlResp);
		assertNull(dlResp.error);
		File downloaded = new File(put.filename + ".dl");
		assertTrue(downloaded.exists());
		assertEquals(LENGTH, downloaded.length());
		File downloaded2 = new File(put2.filename + ".dl");
		assertTrue(downloaded2.exists());
		assertEquals(LENGTH, downloaded2.length());

		Files.deleteIfExists(downloaded.toPath());
		Files.deleteIfExists(downloaded2.toPath());
		Files.deleteIfExists(path);
		Files.deleteIfExists(path2);
	}

	@Test
	public void getManyObjects() throws Exception {
		ISdsBackingStore store = getStore();

		int cnt = 1024;
		List<PutRequest> puts = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++) {
			PutRequest put = new PutRequest();
			put.mailbox = "titi";
			put.guid = UUID.randomUUID().toString();
			Path path = tempContent(256 * 1024);
			put.filename = path.toFile().getAbsolutePath();
			assertNull(store.upload(put).join().error);
			puts.add(put);
			Files.delete(path);
		}

		MgetRequest get = new MgetRequest();
		get.mailbox = "titi";
		get.transfers = puts.stream().map(p -> Transfer.of(p.guid, p.filename + ".dl")).collect(Collectors.toList());
		for (int i = 0; i < 1; i++) {
			SdsResponse dlResp = store.downloads(get).join();
			assertNotNull(dlResp);
			assertNull(dlResp.error);
			for (Transfer t : get.transfers) {
				Files.deleteIfExists(Paths.get(t.filename));
			}
		}
	}

	private Path tempContent() throws IOException {
		return tempContent(LENGTH);
	}

	private Path tempContent(int len) throws IOException {
		Path path = Files.createTempFile("object", ".eml");
		byte[] content = new byte[len];
		ThreadLocalRandom.current().nextBytes(content);
		Files.write(path, content);
		return path;
	}

}

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
package net.bluemind.sds.proxy.store.s3.tests;

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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.proxy.dto.DeleteRequest;
import net.bluemind.sds.proxy.dto.ExistRequest;
import net.bluemind.sds.proxy.dto.ExistResponse;
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.dto.MgetRequest;
import net.bluemind.sds.proxy.dto.MgetRequest.Transfer;
import net.bluemind.sds.proxy.dto.PutRequest;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.s3.S3BackingStoreFactory;

public class S3StoreTests {

	private static String s3Ip;
	public static int LENGTH = 64;

	@BeforeClass
	public static void beforeClass() {
		s3Ip = DockerEnv.getIp("bluemind/s3");
	}

	@Test
	public void createS3Store() {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		JsonObject s3js = config.asJson();
		System.err.println(s3js.encodePrettily());

		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), s3js);
		assertNotNull(store);
	}

	@Test
	public void objectDoesNotExist() {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());
		ExistRequest er = new ExistRequest();
		er.mailbox = "titi";
		er.guid = UUID.randomUUID().toString();
		ExistResponse existsResp = store.exists(er).join();
		assertFalse(existsResp.exists);
	}

	@Test
	public void putObject() throws IOException {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());
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
	}

	@Test
	public void deleteObject() throws IOException {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());

		PutRequest pr = new PutRequest();
		pr.mailbox = "tata";
		pr.guid = UUID.randomUUID().toString();
		pr.filename = tempContent().toFile().getAbsolutePath();
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

	}

	@Test
	public void getObject() throws IOException {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());

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
	}

	@Test
	public void getObjects() throws IOException {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());

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
	}

	@Test
	public void getManyObjects() throws IOException {
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://" + s3Ip + ":8000",
				"junit-" + System.currentTimeMillis());
		ISdsBackingStore store = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());

		int cnt = 100;
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
		for (int i = 0; i < cnt; i++) {
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

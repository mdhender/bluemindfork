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
package net.bluemind.core.backup.continuous.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.hash.Hashing;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.kafka.container.ZkKafkaContainer;

public class DefaultBackupStoreTests {

	private static ZkKafkaContainer kafka;

	@BeforeClass
	public static void before() {
		kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		DefaultLeader.reset();
	}

	@AfterClass
	public static void after() {
		DefaultLeader.leader().releaseLeadership();

		kafka.stop();

		System.clearProperty("bm.mcast.id");
		InstallationId.reload();
	}

	@Test
	public void testWriteSomething() {
		IBackupStoreFactory store = DefaultBackupStore.store();
		assertNotNull("store must not be null", store);
		long time = 1;
		ItemValue<Foo> rand = ItemValue.create("yeah" + time, Foo.random());
		rand.internalId = time;
		BaseContainerDescriptor container = BaseContainerDescriptor.create("dir_devenv.blue", "devenv.blue directory",
				"system", "dir", "devenv.blue", true);
		IBackupStore<Foo> topic = store.forContainer(container);
		assertNotNull("topic must not be null", topic);
		System.err.println("got topic " + topic);
		topic.store(rand);

		IBackupStore<Foo> another = store.forContainer(container);
		for (time = 2; time < 20; time++) {
			rand = ItemValue.create("yeah" + time, Foo.random());
			rand.internalId = time;
			another.store(rand);
		}

	}

	@Test
	public void testInstallationIdTweaks() {
		// 0536412c415145adbdb316e91e2b8f55
		String iid = "bluemind-0536412c-4151-45ad-bdb3-16e91e2b8f55";
		System.setProperty("bm.mcast.id", iid);
		InstallationId.reload();
		String setIid = InstallationId.getIdentifier();
		System.err.println("setiid: " + setIid);
		assertEquals(iid, setIid);
		IBackupStoreFactory store = DefaultBackupStore.store();
		IBackupReader reader = DefaultBackupStore.reader();
		assertNotNull("store must not be null", store);
		BaseContainerDescriptor container = BaseContainerDescriptor.create(
				"bluemind-0536412c-4151-45ad-bdb3-16e91e2b8f55", "install Moisie", "system", "installation", null,
				true);
		RecordKey expectedKey = randomKey();
		ItemValue<RecordKey> rand = ItemValue.create("yeah42", expectedKey);
		IBackupStore<RecordKey> topic = store.forContainer(container);
		topic.store(rand);

		Collection<String> insts = reader.installations();
		assertTrue(insts.contains(iid));
		ILiveBackupStreams content = reader.forInstallation(iid);
		ILiveStream orphans = content.orphans();
		assertNotNull(orphans);
	}

	@Test
	public void testReReadSomething() throws InterruptedException {
		IBackupStoreFactory store = DefaultBackupStore.store();
		IBackupReader reader = DefaultBackupStore.reader();
		assertNotNull("store must not be null", store);
		System.err.println("LEADER: " + store.leadership().isLeader());
		long time = 1;
		RecordKey expectedKey = randomKey();
		ItemValue<RecordKey> rand = ItemValue.create("yeah" + time, expectedKey);
		rand.internalId = time;
		BaseContainerDescriptor container = BaseContainerDescriptor.create("install_moisie", "install Moisie", "system",
				"installation", null, true);
		IBackupStore<RecordKey> topic = store.forContainer(container);
		assertNotNull("topic must not be null", topic);

		topic.store(rand);

		System.err.println("Tried to store to " + topic + " from " + store);

		Collection<String> installs = reader.installations();
		assertEquals(1, installs.size());
		String iid = installs.iterator().next();
		System.err.println("iid: " + iid);
		ILiveStream stream = reader.forInstallation(iid).orphans();
		System.err.println("On " + stream);
		stream.subscribe(null, de -> {
			System.err.println("Getting something " + stream);
			System.err.println(de + " " + de.key.uid + " " + de.key.owner);
			assertTrue(de.payload.length > 0);
			JsonObject parsed = new JsonObject(new String(de.payload));
			System.err.println(parsed.encodePrettily());
			assertTrue(parsed.containsKey("producedBy"));
			assertTrue(parsed.containsKey("valueClass"));

		});

	}

	private RecordKey randomKey() {
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		RecordKey k = new RecordKey();
		k.type = Hashing.goodFastHash(64).hashLong(rand.nextLong()).toString();
		return k;
	}

}

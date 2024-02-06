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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.store.kafka.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.backup.continuous.store.ITopicStore.DefaultTopicDescriptor;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.ITopicStore.TopicDescriptor;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.core.backup.store.kafka.KafkaTopicStore;
import net.bluemind.core.backup.store.kafka.metrics.KafkaMetric;
import net.bluemind.core.backup.store.kafka.metrics.KafkaTopicMetrics;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;

public class KafkaTopicStoreTests {

	private ZkKafkaContainer container;
	private KafkaTopicStore store;

	@Before
	public void before() {
		this.container = new ZkKafkaContainer();
		container.start();
		String ip = container.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		System.err.println("Container started on " + ip);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		this.store = new KafkaTopicStore();
		assertNotNull(store);

	}

	@After
	public void after() {

		store.flushAll();
		System.err.println("*** flush all done ***");

		if (container != null) {
			container.close();
		}

		KafkaTopicMetrics.get().clearAllPublishMetrics();
	}

	@Test(timeout = 30000)
	public void testStartStore() throws Exception {
		assertTrue(store.isEnabled());

		TopicDescriptor descriptor = DefaultTopicDescriptor.of("bluemind-toto/dom.com/owner/calendar/a_cal_uid");
		TopicPublisher laFileDuBedouin = store.getPublisher(descriptor);
		assertNotNull(laFileDuBedouin);

		TopicPublisher another = store.getPublisher(descriptor);
		assertEquals(laFileDuBedouin, another);

		int cnt = 1000;
		long time = System.currentTimeMillis();
		CompletableFuture<?>[] comps = new CompletableFuture<?>[cnt];
		for (int i = 0; i < cnt; i++) {
			comps[i] = another.store("any", "key".getBytes(), ("yeah " + Long.toString(i)).getBytes());
		}
		CompletableFuture<Void> global = CompletableFuture.allOf(comps);
		global.get(30, TimeUnit.SECONDS);
		time = System.currentTimeMillis() - time;
		System.err.println("completed " + cnt + " in " + time + "ms.");

		long fetch = System.currentTimeMillis();
		AtomicInteger count = new AtomicInteger();
		TopicSubscriber subscriber = store.getSubscriber("toto-dom.com");
		IResumeToken token = subscriber.subscribe((key, value, p, o) -> {
			count.incrementAndGet();
			System.err.println("de.payload: " + value);
		});
		fetch = System.currentTimeMillis() - fetch;
		System.err.println("Fetched " + count.get() + " in " + fetch + "ms.");
		assertEquals(cnt, count.get());
		assertNotNull(token);
		System.err.println("token: " + token);

		fetch = System.currentTimeMillis();
		System.err.println("full refetch....");
		AtomicInteger recount = new AtomicInteger();
		subscriber.subscribe((key, value, p, o) -> {
			recount.incrementAndGet();
		});
		fetch = System.currentTimeMillis() - fetch;
		System.err.println("Fetched " + recount.get() + " in " + fetch + "ms.");

		// resume
		AtomicBoolean called = new AtomicBoolean();
		subscriber.subscribe(token, (key, value, p, o) -> {
			System.err.println("got " + key);
			called.set(true);
		});
		assertFalse(called.get());

		laFileDuBedouin.store("any-partition", "key2".getBytes(), "another".getBytes()).join();

		subscriber.subscribe(token, (key, value, p, o) -> {
			System.err.println("got " + key);
			assertEquals("key2", new String(key));
			called.set(true);
		});
		assertTrue(called.get());
	}

	@Test(timeout = 30000)
	public void testListTopicNames() {
		TopicPublisher orphansPublisher = store
				.getPublisher(DefaultTopicDescriptor.of("inst/__orphans__/owner/type/uid2"));
		assertNotNull(orphansPublisher);
		TopicPublisher domainPublisher = store.getPublisher(DefaultTopicDescriptor.of("inst/dom/owner/type/uid2"));
		assertNotNull(domainPublisher);
		domainPublisher.store("any-partition", "key".getBytes(), "yeah".getBytes());

		Set<String> topicNames = store.topicNames();
		assertEquals(2, topicNames.size());
		String domainTopicName = topicNames.stream().filter(name -> name.equals("inst-dom")).findFirst().orElse(null);
		assertNotNull(domainTopicName);
		String orphansTopicName = topicNames.stream().filter(name -> name.equals("inst-__orphans__")).findFirst()
				.orElse(null);
		assertNotNull(orphansTopicName);

		AtomicBoolean called = new AtomicBoolean();
		store.getSubscriber(domainTopicName).subscribe((key, value, p, o) -> {
			called.set(true);
		});
		assertTrue(called.get());
	}

	@Test(timeout = 30000)
	public void testMetrics_producer() throws Exception {

		assertTrue(store.isEnabled());

		TopicDescriptor descriptor = DefaultTopicDescriptor.of("bluemind-toto/dom.com/owner/calendar/a_cal_uid");
		TopicPublisher another = store.getPublisher(descriptor);
		assertNotNull(another);

		int cnt = 1000;
		ExecutorService executor = Executors.newFixedThreadPool(cnt);
		CompletableFuture<?>[] comps = new CompletableFuture<?>[cnt];
		CompletableFuture<?>[] compss = new CompletableFuture<?>[cnt];
		for (int i = 0; i < cnt; i++) {
			comps[i] = CompletableFuture.supplyAsync(() -> publishMessage("any", ("key").getBytes(), another),
					executor);
			compss[i] = CompletableFuture.supplyAsync(() -> publishMessage("any" + UUID.randomUUID().toString(),
					("key" + UUID.randomUUID().toString()).getBytes(), another), executor);
		}
		CompletableFuture.allOf(comps).orTimeout(10, TimeUnit.MINUTES).join();
		CompletableFuture.allOf(compss).orTimeout(10, TimeUnit.MINUTES).join();

		Thread.sleep(3000);

		KafkaTopicMetrics.get().publish();
		List<KafkaMetric> publishMetrics = KafkaTopicMetrics.get().getPublishMetrics();
		assertNotNull(publishMetrics);
		assertFalse(publishMetrics.isEmpty());
		assertTrue(publishMetrics.size() > cnt - 100 && publishMetrics.size() < cnt + 100);
		assertTrue(publishMetrics.stream().allMatch(m -> m.client.equals("PRODUCER")));
	}

	private CompletableFuture<Void> publishMessage(String partitionKey, byte[] bs, TopicPublisher another) {
		return another.store(partitionKey, bs, ("yeah " + UUID.randomUUID().toString()).getBytes());
	}

	@Test(timeout = 30000)
	public void testMetrics_consumer() throws Exception {

		assertTrue(store.isEnabled());

		TopicDescriptor descriptor = DefaultTopicDescriptor.of("bluemind-toto/dom.com/owner/calendar/a_cal_uid");
		TopicPublisher another = store.getPublisher(descriptor);
		assertNotNull(another);

		int cnt = 1000;
		ExecutorService executor = Executors.newFixedThreadPool(cnt);
		CompletableFuture<?>[] comps = new CompletableFuture<?>[cnt];
		for (int i = 0; i < cnt; i++) {
			comps[i] = CompletableFuture.supplyAsync(() -> publishMessage("any" + UUID.randomUUID().toString(),
					("key" + UUID.randomUUID().toString()).getBytes(), another), executor);
		}
		CompletableFuture.allOf(comps).orTimeout(10, TimeUnit.MINUTES).join();

		TopicSubscriber subscriber = store.getSubscriber("toto-dom.com");
		AtomicBoolean called = new AtomicBoolean();
		subscriber.subscribe((key, value, p, o) -> {
			called.set(true);
		});
		assertTrue(called.get());

		KafkaTopicMetrics.get().publish();
		List<KafkaMetric> publishMetrics = KafkaTopicMetrics.get().getPublishMetrics();
		assertNotNull(publishMetrics);
		assertFalse(publishMetrics.isEmpty());
		assertTrue(publishMetrics.stream().anyMatch(m -> m.client.equals("CONSUMER")));

	}

//	@Test
//	public void testByContainerFiltering() {
//		KafkaTopicStore store = new KafkaTopicStore();
//		TopicPublisher firstOwner = store
//				.get(DefaultTopicDescriptor.of("bluemind-inst-tata-tutu/dom/owner1/type/uid1"));
//		RecordKey key = buildKey(firstOwner, 12, "String");
//		firstOwner.store(key, "uid1".getBytes()).join();
//		TopicPublisher sameTypeOtherCont = store
//				.get(DefaultTopicDescriptor.of("bluemind-inst-tata-tutu/dom/owner2/type/uid2"));
//		key = buildKey(sameTypeOtherCont, 13, "LinkedHashMap");
//		sameTypeOtherCont.store(key, "uid2".getBytes()).join();
//
//		AtomicInteger firstCount = new AtomicInteger();
//		firstOwner.subscribe(de -> firstCount.incrementAndGet());
//		assertEquals(1, firstCount.get());
//
//		AtomicInteger secCount = new AtomicInteger();
//		sameTypeOtherCont.subscribe(de -> secCount.incrementAndGet());
//		assertEquals(1, secCount.get());
//
//		AtomicInteger total1 = new AtomicInteger();
//		sameTypeOtherCont.subscribe(null, de -> total1.incrementAndGet(), k -> true);
//		assertEquals(2, total1.get());
//
//		AtomicInteger total2 = new AtomicInteger();
//		IResumeToken tok = firstOwner.subscribe(null, de -> total2.incrementAndGet(), k -> true);
//		assertEquals(2, total2.get());
//
//		AtomicInteger total3 = new AtomicInteger();
//		sameTypeOtherCont.subscribe(tok, de -> total3.incrementAndGet(), k -> true);
//		assertEquals(0, total3.get());
//	}

}

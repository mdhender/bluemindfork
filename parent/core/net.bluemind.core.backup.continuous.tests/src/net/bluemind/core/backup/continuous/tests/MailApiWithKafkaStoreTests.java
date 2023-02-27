/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.backup.continuous.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.Handler;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.kafka.container.ZkKafkaContainer;

public class MailApiWithKafkaStoreTests extends MailApiTestsBase {

	private ZkKafkaContainer kafka;

	@Override
	public void before() throws Exception {
		kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		DefaultLeader.reset();

		super.before();
	}

	private String rand() {
		byte[] tgt = new byte[4];
		ThreadLocalRandom.current().nextBytes(tgt);
		return ByteBufUtil.hexDump(tgt);
	}

	private InputStream randomEml() {
		String cnt = """
				From: <sender>
				Subject: <sub>
				Message-Id: <mid>

				This is a <token> in body.
				""";
		cnt = cnt.replace("\n", "\r\n")//
				.replace("<sender>", "doe." + rand() + "@lost" + rand() + ".lan")//
				.replace("<mid>", "<mid." + rand() + "." + rand() + "@lost.lan>")//
				.replace("<sub>", "Topic is " + rand())//
				.replace("<token>", rand());
		return new ByteArrayInputStream(cnt.getBytes());
	}

	public static class TestHandler implements Handler<DataElement> {

		Map<String, AtomicInteger> byType = new ConcurrentHashMap<>();

		@Override
		public void handle(DataElement event) {
			// System.err.println(event.part + ":" + event.offset + " -> k: " +
			// event.key.type);
			AtomicInteger count = byType.computeIfAbsent(event.key.type, k -> new AtomicInteger());
			count.incrementAndGet();
			if (event.key.type.equals(IMailReplicaUids.REPLICATED_MBOXES)) {
				System.err.println("box: " + event.key);
			}
		}

	}

	@Test
	public void testFreshMessageWithKafkaHooks() {
		String esType = "message_bodies_es_source";

		Integer added;
		added = imapAsUser(sc -> {
			int uid = sc.append("INBOX", randomEml(), new FlagsList());
			sc.uidStore(Collections.singletonList(uid), FlagsList.of(Flag.SEEN), true);
			return uid;
		});
		assertNotNull(added);
		assertTrue(added > 0);

		ILiveStream domStream = DefaultBackupStore.reader().forInstallation(InstallationId.getIdentifier()).domains()
				.stream().filter(ls -> ls.domainUid().equals(domUid)).findAny().orElseThrow();
		TestHandler th = new TestHandler();
		IResumeToken rt = domStream.subscribe(th);
		System.err.println("rt: " + rt);
		int esSources = th.byType.get(esType).get();
		assertEquals(1, esSources);
	}

	@Override
	public void after() throws Exception {
		kafka.stop();

		super.after();
	}

}

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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.common.task.Tasks;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class DeletionsWithKafkaTests extends MailApiWithKafkaBaseTests {

	@Override
	public void before() throws Exception {
		// fast compaction
		System.setProperty("kafka.topic.maxCompactionLag", "5s");
		System.setProperty("kafka.topic.maxSegmentDuration", "5s");

		super.before();
	}

	@Test
	public void testFreshUserThenDeletion() throws InterruptedException {

		ItemValue<User> fresh = sharedUser("delme", domUid, userUid);
		assertNotNull(fresh);

		ILiveBackupStreams reRead = DefaultBackupStore.reader().forInstallation(InstallationId.getIdentifier());
		ILiveStream domStream = reRead.domains().stream().filter(ls -> ls.domainUid().equals(domUid)).findAny()
				.orElseThrow();

		LongAdder ownedKeys = checkOwnedContent(fresh.uid, domStream);
		assertTrue(ownedKeys.sum() > 0);

		IUser userApi = serverProv.instance(IUser.class, domUid);
		TaskRef tr = userApi.delete(fresh.uid);
		Logger delUserLog = LoggerFactory.getLogger("delUser");
		TaskStatus status = Tasks.followStream(serverProv, delUserLog, "deletion", tr, true)
				.orTimeout(1, TimeUnit.MINUTES).join();
		assertTrue(status.state.succeed);

		triggerKafkaCompaction(fresh.uid);

		LongAdder postDelKeys = checkOwnedContent(fresh.uid, domStream);
		System.err.println("postDel " + postDelKeys.sum());
	}

	private LongAdder checkOwnedContent(String ownerUid, ILiveStream domStream) {
		LongAdder ownedKeys = new LongAdder();
		Set<String> distinctKeys = new LinkedHashSet<>();
		IResumeToken token = domStream.subscribe(de -> {
			RecordKey copy = new RecordKey(de.key.type, de.key.owner, de.key.uid, de.key.id, de.key.valueClass, "NOOP");

			if (ownerUid.equals(de.key.owner)) {
				if (de.key.operation.equals("DELETE")) {
					distinctKeys.remove(copy.toString());
				} else if (de.payload != null) {
					distinctKeys.add(copy.toString());
				}
				// System.err.println("k: " + de.key + " " + (de.payload == null ? "(DEL)" :
				// "WITH_VALUE"));
				ownedKeys.add(1);
			}
		});
		System.err.println("Got token: " + token);
		System.err.println("Owner " + ownerUid + " has " + ownedKeys.sum() + " in kafka.");
		System.err.println("Owner has " + distinctKeys.size() + " distinct keys");
		for (String s : distinctKeys) {
			System.err.println("        Distinct " + s);
		}

		return ownedKeys;
	}

	private void triggerKafkaCompaction(String owner) throws InterruptedException {
		System.err.println("Sleeping until segment.ms occurs");
		Thread.sleep(6000);

		// write something for segment roll-over
		BaseContainerDescriptor bcd = BaseContainerDescriptor.create("fake_" + owner, "nom-bidon", "type-bidon", owner,
				domUid, true);
		IBackupStore<Foo> tgt = DefaultBackupStore.store().forContainer(bcd);
		byte[] fat = new byte[32768];
		ThreadLocalRandom.current().nextBytes(fat);
		Foo foo = new Foo();
		foo.blob = fat;
		for (int i = 0; i < 4; i++) {
			ItemValue<Foo> pill = ItemValue.create("fake" + System.currentTimeMillis() + "." + i, new Foo());
			pill.value.bar = pill.uid;
			tgt.delete(pill).orTimeout(10, TimeUnit.SECONDS).join();
			Thread.sleep(1000);
			System.err.println(".... try to compact ....");
		}
		System.err.println("Compaction should have occurred.");
	}

}

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

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupManager;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.kafka.container.ZkKafkaContainer;

public abstract class MailApiWithKafkaBaseTests extends MailApiTestsBase {

	private ZkKafkaContainer kafka;

	@Override
	@BeforeEach
	public void before(TestInfo info) throws Exception {
		kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		DefaultLeader.reset();

		super.before(info);
	}

	@Override
	@AfterEach
	public void after(TestInfo info) throws Exception {
		dropTopics();

		DefaultLeader.leader().releaseLeadership();
		System.clearProperty("bm.kafka.bootstrap.servers");
		System.clearProperty("bm.zk.servers");
		DefaultLeader.reset();

		kafka.stop();
		kafka.close();

		super.after(info);
	}

	private void dropTopics() {
		IBackupReader reader = DefaultBackupStore.reader();
		IBackupManager manager = DefaultBackupStore.manager();
		Collection<String> insts = reader.installations();
		for (String iid : insts) {
			for (ILiveStream stream : reader.forInstallation(iid).listAvailable()) {
				System.err.println("delete " + stream);
				manager.delete(stream);
			}
		}
		System.err.println("finished publisher reset part.");
	}

}

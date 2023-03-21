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

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.kafka.container.ZkKafkaContainer;

public abstract class MailApiWithKafkaBaseTests extends MailApiTestsBase {

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

	@Override
	public void after() throws Exception {
		kafka.stop();
		kafka.close();

		super.after();
	}

}

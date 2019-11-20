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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.kafka.client.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.Test;

import net.bluemind.kafka.container.ZkKafkaContainer;

public class KafkaConnectTests {

	@Test
	public void testAdminConnect() {
		try (ZkKafkaContainer container = new ZkKafkaContainer()) {
			container.start();
			String ip = container.inspectAddress();
			// new NetworkHelper(ip).waitForListeningPort(9092, 10, TimeUnit.SECONDS);
			Properties properties = new Properties();
			properties.put("bootstrap.servers", ip + ":" + container.port());
			properties.put("client.id", "junit-" + getClass().getCanonicalName() + "-adminConnect");

			String topic = "junit." + System.currentTimeMillis();
			try (AdminClient admin = AdminClient.create(properties)) {
				assertNotNull(admin);
				Set<String> topics = admin.listTopics(new ListTopicsOptions().listInternal(true)).names().get(10,
						TimeUnit.SECONDS);
				System.err.println("Topics: " + topics);
				NewTopic nt = new NewTopic(topic, 3, (short) 1);
				admin.createTopics(Arrays.asList(nt)).all().get(5, TimeUnit.SECONDS);
				topics = admin.listTopics(new ListTopicsOptions().listInternal(true)).names().get(10, TimeUnit.SECONDS);
				System.err.println("Topics: " + topics);
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

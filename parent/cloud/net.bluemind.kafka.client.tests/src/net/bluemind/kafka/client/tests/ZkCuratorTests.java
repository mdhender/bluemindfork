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
package net.bluemind.kafka.client.tests;

import static org.junit.Assert.fail;

import java.util.List;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;

import net.bluemind.kafka.container.ZkKafkaContainer;

public class ZkCuratorTests {

	@Test
	public void testCuratorConnect() {
		try (ZkKafkaContainer container = new ZkKafkaContainer()) {
			container.start();
			String ip = container.inspectAddress();
			System.err.println("Will try zk connect to " + ip);
			;
			try (CuratorFramework client = buildFramework(ip)) {
				client.start();
				System.err.println("client started...");
				List<String> content = client.getChildren().forPath("/");
				for (String s : content) {
					System.err.println(" * " + s);
				}
				client.create().creatingParentsIfNeeded().forPath("/bm/junit" + System.currentTimeMillis(),
						"yeah".getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private CuratorFramework buildFramework(String ip) {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder().connectString(ip + ":2181").retryPolicy(retryPolicy).build();
	}

}

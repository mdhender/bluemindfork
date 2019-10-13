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
package net.bluemind.forest.cloud.hazelcast.tests;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;

import net.bluemind.forest.cloud.hazelcast.HzStarter;
import net.bluemind.kafka.container.ZkKafkaContainer;

public class StartupHzTests {

	@Test
	public void testStart() throws InterruptedException, ExecutionException, TimeoutException {
		try (ZkKafkaContainer container = new ZkKafkaContainer()) {
			container.start();
			String ip = container.inspectAddress();

			HzStarter starter = new HzStarter("junit", ip);
			HazelcastInstance hz = starter.get(10, TimeUnit.SECONDS);
			assertNotNull(hz);
			hz.shutdown();
		}
	}

}

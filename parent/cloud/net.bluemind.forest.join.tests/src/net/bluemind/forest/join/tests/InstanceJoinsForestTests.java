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
package net.bluemind.forest.join.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.Token;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.forest.cloud.api.ForestTopology;
import net.bluemind.forest.cloud.api.IForestJoin;
import net.bluemind.forest.cloud.api.Instance;
import net.bluemind.forest.cloud.api.Instance.Node;
import net.bluemind.forest.cloud.api.Instance.Partition;
import net.bluemind.forest.cloud.api.Instance.Version;
import net.bluemind.kafka.container.SingleKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;

public class InstanceJoinsForestTests {

	private IForestJoin cloudJoinApiClient;

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		SingleKafkaContainer.get();
		System.err.println("After kafka");
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		cloudJoinApiClient = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", null)
				.instance(IForestJoin.class, "mail.gouv.fr");
	}

	@Test
	public void simulateJoinProcess() throws InterruptedException {
		Instance instance = new Instance();
		instance.aliases = Arrays.asList(//
				Partition.create("devenv.blue", "mail.gouv.fr"), //
				Partition.create("devenv.red", "mail.gouv.fr")//
		);
		instance.installationId = "plan-b-41";
		instance.coreToken = Token.admin0();
		instance.topology = Arrays.asList(Node.create("single", "127.0.0.1", "bm/core", "mail/imap"));
		instance.externalUrl = "http://127.0.0.1:8090/";
		instance.version = Version.create(4, 1, 66666);
		ForestTopology forestTopo = cloudJoinApiClient.handshake(instance);
		assertNotNull(forestTopo);
		System.err.println("topo is : " + forestTopo.broker.address);

		Thread.sleep(2000);
		System.err.println("============= JOIN COMPLETED =============");

//		System.err.println("*** trigger a second time ***");
//		instance.installationId = "plan-b-sans-accroc";
//		forestTopo = cloudJoinApiClient.handshake(instance);
//		assertNotNull(forestTopo);

	}

}

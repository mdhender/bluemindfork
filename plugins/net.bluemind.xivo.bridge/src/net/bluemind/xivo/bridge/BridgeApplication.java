/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.xivo.bridge;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.consumer.ConsumerStart;
import net.bluemind.xivo.bridge.http.v1.HornetQBridge;
import net.bluemind.xivo.bridge.http.v1.HttpEndpointV1Router;
import net.bluemind.xivo.bridge.impl.DepDoneHandler;

public class BridgeApplication implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(BridgeApplication.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting XIVO Bridge...");

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				MQ.registerProducer(Topic.XIVO_PHONE_STATUS);

				Vertx pm = VertxPlatform.getVertx();

				DepDoneHandler depDone = new DepDoneHandler();
				int procs = Runtime.getRuntime().availableProcessors();
				int instances = Math.max(10, procs);

				pm.deployVerticle(HttpEndpointV1Router::new, new DeploymentOptions().setInstances(instances), depDone);
				pm.deployVerticle(HornetQBridge::new, new DeploymentOptions().setInstances(instances).setWorker(true),
						depDone);
				pm.deployVerticle(ConsumerStart::new, new DeploymentOptions().setInstances(1).setWorker(true), depDone);

			}
		});

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}

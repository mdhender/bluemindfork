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

import java.net.URL;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.PlatformManager;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
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

				PlatformManager pm = VertxPlatform.getPlatformManager();

				DepDoneHandler depDone = new DepDoneHandler();
				int procs = Runtime.getRuntime().availableProcessors();
				int instances = Math.max(10, procs);
				pm.deployVerticle(HttpEndpointV1Router.class.getCanonicalName(), null, new URL[0], instances, null,
						depDone);

				pm.deployWorkerVerticle(false, HornetQBridge.class.getCanonicalName(), null, new URL[0], instances,
						null, depDone);

			}
		});

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}

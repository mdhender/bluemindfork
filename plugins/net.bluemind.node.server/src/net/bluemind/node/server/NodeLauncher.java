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
package net.bluemind.node.server;

import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.server.busmod.SysCommand;
import net.bluemind.node.server.handlers.DepDoneHandler;
import net.bluemind.node.server.timers.TikaMonitor;
import net.bluemind.systemd.notify.SystemD;

public class NodeLauncher implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(NodeLauncher.class);
	private long tikaTimer;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting BlueMind Node on port {}...", Activator.NODE_PORT);
		Vertx pm = VertxPlatform.getVertx();

		int procs = Runtime.getRuntime().availableProcessors();
		int instances = Math.max(10, procs);
		DepDoneHandler httpDep = new DepDoneHandler();
		pm.deployVerticle(BlueMindNode::new, new DeploymentOptions().setInstances(instances), httpDep);

		DepDoneHandler workerDep = new DepDoneHandler();
		pm.deployVerticle(SysCommand::new, new DeploymentOptions().setInstances(1), workerDep);

		this.tikaTimer = pm.setPeriodic(10000, new TikaMonitor());

		httpDep.await();
		workerDep.await();

		VertxPlatform.spawnBlocking(5, TimeUnit.SECONDS);

		if (SystemD.isAvailable()) {
			SystemD.get().notifyReady();
		}
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		Vertx vertx = VertxPlatform.getVertx();
		vertx.cancelTimer(tikaTimer);
		logger.info("Stopping BlueMind Node {}...", this);
		VertxPlatform.undeployVerticles(ar -> {
		});
	}

}

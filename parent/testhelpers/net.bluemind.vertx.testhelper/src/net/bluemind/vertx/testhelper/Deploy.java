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
package net.bluemind.vertx.testhelper;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformManager;

import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.testhelper.impl.DoneHandler;

public class Deploy {

	private final static PlatformManager pm = VertxPlatform.getPlatformManager();
	private static final Logger logger = LoggerFactory.getLogger(Deploy.class);

	public static void afterTest(Set<String> deploymentIDs) {
		try {
			undeploy(deploymentIDs, 5, TimeUnit.SECONDS);
			deploymentIDs.clear();
		} catch (Exception e) {
			logger.error("Undeployement issue " + deploymentIDs + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Deploy verticles by classname & return a promise of deployement ids
	 * 
	 * @param worker
	 * @param classes
	 * @return
	 */
	public static CompletableFuture<Set<String>> verticles(boolean worker, String... classes) {
		return verticles(worker, Arrays.asList(classes));
	}

	/**
	 * Deploy verticles by classname & return a promise of deployement ids
	 * 
	 * @param worker
	 * @param classes
	 * @return
	 */
	public static CompletableFuture<Set<String>> verticles(boolean worker, Collection<String> classes) {
		DoneHandler<String> done = new DoneHandler<>(classes.size());
		for (String klass : classes) {
			if (worker) {
				pm.deployWorkerVerticle(true, klass, new JsonObject(), new URL[0], 1, null, done);
			} else {
				pm.deployVerticle(klass, new JsonObject(), new URL[0], 1, null, done);
			}
		}
		return done.promise();
	}

	private static void undeploy(Set<String> deployed, long t, TimeUnit tu)
			throws InterruptedException, ExecutionException, TimeoutException {
		DoneHandler<Void> done = new DoneHandler<>(deployed.size());
		logger.info("Undeploying {}", deployed.size());
		for (String s : deployed) {
			logger.info("Undeploy {}", s);
			pm.undeploy(s, done);
		}
		done.promise().get(t, tu);
	}
}

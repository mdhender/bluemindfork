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
package net.bluemind.eas.testhelper.vertx;

import java.util.HashSet;
import java.util.Set;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import net.bluemind.lib.vertx.VertxPlatform;

public class Deploy {

	private static Vertx pm = VertxPlatform.getVertx();

	public interface VerticleConstructor {
		Verticle create();

		public static VerticleConstructor[] of(VerticleConstructor... constructors) {
			return constructors;
		}
	}

	public static Set<String> beforeTest(VerticleConstructor[] classes, VerticleConstructor[] workerClasses) {
		Set<String> deploymentIDs = new HashSet<>();
		deploy(deploymentIDs, classes, false);
		deploy(deploymentIDs, workerClasses, true);
		return deploymentIDs;
	}

	public static void afterTest(Set<String> deploymentIDs) {
		undeploy(deploymentIDs);
	}

	private static void deploy(Set<String> deployed, VerticleConstructor[] classes, boolean worker) {
		DoneHandler<String> done = new DoneHandler<>(classes.length);
		for (int i = 0; i < classes.length; i++) {
			final int idx = i;
			if (worker) {
				pm.deployVerticle(() -> classes[idx].create(), new DeploymentOptions().setWorker(true).setInstances(1),
						done);
			} else {
				pm.deployVerticle(() -> classes[idx].create(), new DeploymentOptions().setInstances(1), done);
			}
		}
		deployed.addAll(done.waitForIt());
	}

	private static void undeploy(Set<String> deployed) {
		DoneHandler<Void> done = new DoneHandler<>(deployed.size());
		for (String s : deployed) {
			pm.undeploy(s, done);
		}
		done.waitForIt();
	}
}

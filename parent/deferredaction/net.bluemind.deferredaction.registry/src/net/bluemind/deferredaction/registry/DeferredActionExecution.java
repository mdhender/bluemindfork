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
package net.bluemind.deferredaction.registry;

import java.time.ZonedDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class DeferredActionExecution extends AbstractVerticle {

	private static final Executor executor = Executors.newSingleThreadExecutor();
	private static final Logger logger = LoggerFactory.getLogger(DeferredActionExecution.class);

	@Override
	public void start() {
		super.vertx.setPeriodic(TimeUnit.MINUTES.toMillis(5), this::execute);
	}

	private void execute(Long timerId) {
		if (StateContext.getState() == SystemState.CORE_STATE_RUNNING) {
			executor.execute(() -> {
				DeferredActionPluginLoader.executors.forEach(executor -> {
					logger.debug("Executing deferred action executor {}", executor.getSupportedActionId());
					try {
						executor.create().execute(ZonedDateTime.now().plusMinutes(5));
					} catch (Exception e) {
						logger.warn("Error while executing deferred actions", e);
					}
				});
			});
		}
	}

}

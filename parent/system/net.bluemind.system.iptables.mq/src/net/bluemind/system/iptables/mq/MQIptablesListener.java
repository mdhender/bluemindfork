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
package net.bluemind.system.iptables.mq;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.iptables.IptablesPath;
import net.bluemind.system.iptables.tools.RulesUpdater;

public class MQIptablesListener extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MQIptablesListener.class);
	private String currentState;

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MQIptablesListener();
		}

	}

	public void start() {
		VertxPlatform.eventBus().consumer(SystemState.BROADCAST, (Message<JsonObject> m) -> {
			stateChanged(m.body().getString("operation"));
		});
	}

	private void stateChanged(String op) {
		if (op.equals(currentState)) {
			return;
		}

		currentState = op;

		if ("core.state.running".equals(op)) {
			try {
				initIptablesScript();
			} catch (Exception t) {
				logger.warn("Unable to initialize BlueMind iptables script !", t);
			}
		}
	}

	private void initIptablesScript() throws ServerFault {

		if (!(new File(IptablesPath.IPTABLES_SCRIPT_PATH)).exists()) {
			logger.info("Initialize BlueMind iptables script on all BlueMind nodes");
			RulesUpdater.updateIptablesScript();
		}
	}
}

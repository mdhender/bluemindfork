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
package net.bluemind.central.reverse.proxy.vertx.impl;

import io.vertx.core.Verticle;
import net.bluemind.central.reverse.proxy.vertx.ConfigHolder;
import net.bluemind.central.reverse.proxy.vertx.SessionManager;
import net.bluemind.lib.vertx.IVerticleFactory;

public class ProxyVerticleFactory implements IVerticleFactory {

	@Override
	public boolean isWorker() {
		return false;
	}

	@Override
	public Verticle newInstance() {
		SessionManager sessions = SessionManager.create(ConfigHolder.config);
		return new ProxyVerticle(ConfigHolder.config, sessions);
	}

}

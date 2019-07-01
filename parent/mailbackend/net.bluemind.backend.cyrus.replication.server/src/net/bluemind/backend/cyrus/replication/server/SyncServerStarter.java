/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.vertx.VertxPlatform;

public class SyncServerStarter implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(SyncServerStarter.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		VertxPlatform.spawnVerticles(result -> {
			if (result.succeeded()) {
				logger.info("Started ok.");
			} else {
				logger.error(result.cause().getMessage(), result.cause());
			}
		});
		return IApplicationContext.EXIT_ASYNC_RESULT;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}

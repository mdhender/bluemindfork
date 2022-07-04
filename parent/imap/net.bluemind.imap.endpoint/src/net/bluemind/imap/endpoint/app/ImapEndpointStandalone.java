/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.app;

import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;

public class ImapEndpointStandalone implements IApplication {

	static {
		System.setProperty("osgi.noShutdown", "true");
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		MQ.init(() -> VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS));
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
	}

}

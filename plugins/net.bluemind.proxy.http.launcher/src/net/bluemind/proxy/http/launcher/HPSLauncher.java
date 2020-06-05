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
package net.bluemind.proxy.http.launcher;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.proxy.http.HttpProxyServer;
import net.bluemind.systemd.notify.Startup;

public class HPSLauncher implements IApplication {

	private HttpProxyServer hps;

	private static final Logger logger = LoggerFactory.getLogger(HPSLauncher.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		this.hps = new HttpProxyServer();
		hps.run();
		Startup.notifyReady();
		logger.info("HPS started on port {}.", hps.getPort());
		MQ.init();
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		hps.stop();
		logger.info("HPS stopped.");
	}

}

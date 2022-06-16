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
package net.bluemind.central.reverse.proxy.launcher;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.central.reverse.proxy.ReverseProxyServer;
import net.bluemind.systemd.notify.Startup;

public class CRPLauncher implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(CRPLauncher.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		ReverseProxyServer crp = new ReverseProxyServer();
		crp.run();
		logger.info("CRP started");
		Startup.notifyReady();
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("Application stopped.");
	}

}

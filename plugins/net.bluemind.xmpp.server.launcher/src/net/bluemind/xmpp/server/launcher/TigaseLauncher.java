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
package net.bluemind.xmpp.server.launcher;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.xmpp.server.Activator;

public class TigaseLauncher implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(TigaseLauncher.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting Blue Mind XMPP server");

		Activator.loadTrick();

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("Stopped.");
	}

}

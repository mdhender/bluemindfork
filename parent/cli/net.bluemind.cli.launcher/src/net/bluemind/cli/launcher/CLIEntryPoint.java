/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.launcher;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIEntryPoint implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(CLIEntryPoint.class);

	public CLIEntryPoint() {
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		try {
			AnsiConsole.systemInstall();
			Version version = context.getBrandingBundle().getVersion();
			
			String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
			if (args.length == 0) {
				System.out.println(
					Ansi.ansi().fgBrightBlue().a("Blue").fgBrightCyan().a("Mind").reset().a(" CLI " + version));
			}
			CLIManager cm = new CLIManager(version);
			cm.processArgs(args);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			AnsiConsole.systemUninstall();
		}
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("CLI stopped.");
	}

}

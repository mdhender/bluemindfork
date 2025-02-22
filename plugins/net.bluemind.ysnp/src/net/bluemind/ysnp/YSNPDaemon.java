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
package net.bluemind.ysnp;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.Token;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.Startup;

/**
 * "You Shall Not Pass" saslauthd daemon
 * 
 * 
 */
public class YSNPDaemon implements IApplication {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Object start(IApplicationContext context) throws Exception {
		YSNPConfiguration conf = YSNPConfiguration.INSTANCE;
		logger.info("YSNP daemon starting {}", conf);
		Files.deleteIfExists(Paths.get(conf.getSocketPath()));
		Files.deleteIfExists(Paths.get(conf.getExpireOkSocketPath()));
		Files.deleteIfExists(Paths.get(conf.getArchivedOkSocketPath()));
		logger.info("UNIX socket will be created on {}", conf.getSocketPath());

		if (!Token.exists()) {
			logger.error("/etc/bm/bm-core.tok does not exists. Can't launch");
			System.exit(1);
		}

		VertxPlatform.spawnBlocking(1, TimeUnit.MINUTES);

		Process p = Runtime.getRuntime()
				.exec(new String[] { "chmod", "777", YSNPConfiguration.INSTANCE.getSocketPath() });
		p.waitFor();

		p = Runtime.getRuntime()
				.exec(new String[] { "chmod", "777", YSNPConfiguration.INSTANCE.getExpireOkSocketPath() });
		p.waitFor();

		p = Runtime.getRuntime()
				.exec(new String[] { "chmod", "777", YSNPConfiguration.INSTANCE.getArchivedOkSocketPath() });
		p.waitFor();

		Startup.notifyReady();

		return EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("YSNP daemon shutdown.");
	}

}

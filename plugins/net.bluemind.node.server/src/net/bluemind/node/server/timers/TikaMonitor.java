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
package net.bluemind.node.server.timers;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;

public final class TikaMonitor implements Handler<Long> {

	private Logger logger = LoggerFactory.getLogger(TikaMonitor.class);

	private static final File respawnScript = new File("/usr/share/bm-tika/bin/check_and_respawn.sh");

	public TikaMonitor() {
	}

	private void monitor(File f) throws IOException {
		if (f.exists()) {
			// run the script
			ProcessBuilder pb = new ProcessBuilder(f.getAbsolutePath());

			pb.redirectErrorStream(true);
			Process pid = pb.start();

			int exit = 1;
			try {
				exit = pid.waitFor();
				if (exit > 0) {
					logger.info("Tika was restarted by {} (code: {})", f.getAbsolutePath(), exit);
				}
			} catch (InterruptedException e) {
				logger.error("cmd: " + f.getAbsolutePath() + ", interrupted");
			}
		}
	}

	@Override
	public void handle(Long event) {
		try {
			monitor(respawnScript);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}

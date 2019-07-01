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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.file.FileSystem;

import net.bluemind.lib.vertx.VertxPlatform;

public class HprofMonitor implements Handler<Long> {

	private static final Logger logger = LoggerFactory.getLogger(HprofMonitor.class);

	@Override
	public void handle(Long event) {
		FileSystem fs = VertxPlatform.getVertx().fileSystem();
		fs.readDir("/var/log", "java_pid[0-9]+.hprof", new Handler<AsyncResult<String[]>>() {

			@Override
			public void handle(AsyncResult<String[]> event) {
				if (event.failed()) {
					logger.error("Failed to check for hprof");
				} else {
					String[] hprofs = event.result();
					if (hprofs.length > 0) {
						logger.error("hprofs detected.");
					}
				}
			}
		});
	}
}

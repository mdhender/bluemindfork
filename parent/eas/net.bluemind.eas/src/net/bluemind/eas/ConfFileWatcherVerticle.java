/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.eas;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.eas.config.global.GlobalConfig;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;

public class ConfFileWatcherVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(ConfFileWatcherVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new ConfFileWatcherVerticle();
		}

	}

	@Override
	public void start() {
		try {
			GlobalConfig.configUpdate();
			WatchService watchService = FileSystems.getDefault().newWatchService();
			Paths.get(GlobalConfig.ROOT).register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

			VertxPlatform.executeBlockingPeriodic(20000, tid -> poll(watchService));
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private static void poll(WatchService watchService) {
		try {
			WatchKey watchKeyPolicies = watchService.poll();
			if (watchKeyPolicies == null) {
				return;
			}
			for (WatchEvent<?> event : watchKeyPolicies.pollEvents()) {
				logger.debug("Event kind: {}. File affected: {}.", event.kind(), event.context());
				GlobalConfig.configUpdate();
			}
			watchKeyPolicies.reset();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

}

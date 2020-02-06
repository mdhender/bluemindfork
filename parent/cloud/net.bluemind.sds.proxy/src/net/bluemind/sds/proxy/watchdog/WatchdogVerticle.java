/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.watchdog;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.systemd.notify.SystemD;

public class WatchdogVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(WatchdogVerticle.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new WatchdogVerticle();
		}

	}

	private void setupWatchdog() {
		if (SystemD.isAvailable()) {
			vertx.setPeriodic(10000, tid -> {
				// to check our watchdog works as expected
				File f = new File("/root/sds-proxy.trigger");
				if (f.exists()) {
					vertx.cancelTimer(tid);
					f.delete();
				} else {
					SystemD.get().watchdogKeepalive();
				}
			});
		} else {
			logger.warn("SystemD is not available, not starting watchdog");
		}
	}

	@Override
	public void start() {
		setupWatchdog();
	}

}

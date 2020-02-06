package net.bluemind.tika.server.impl;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.systemd.notify.SystemD;

public final class SystemdWatchdogVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SystemdWatchdogVerticle.class);

	public SystemdWatchdogVerticle() {
		logger.info("created");
	}

	public void start() {
		SystemD.get().setupWatchdog(20, TimeUnit.SECONDS);
		vertx.setPeriodic(10000, tid -> {
			// to check our watchdog works as expected
			File f = new File("/root/tika.trigger");
			if (f.exists()) {
				vertx.cancelTimer(tid);
				f.delete();
			} else {
				SystemD.get().watchdogKeepalive();
			}
		});
		logger.info("Watchdog started.");
	}

}

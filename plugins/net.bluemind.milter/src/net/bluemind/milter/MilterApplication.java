package net.bluemind.milter;

import java.util.concurrent.CompletableFuture;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.systemd.notify.SystemD;

public class MilterApplication implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(MilterApplication.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting MILTER...");
		if (SystemD.isAvailable()) {
			SystemD.get().notifyReady();
		}

		launchVerticles().whenComplete((v, ex) -> {
			if (ex != null) {
				logger.warn("Cannot spawn verticles", ex);
			} else {
				logger.info("Startup complete.");
			}

		});

		return IApplication.EXIT_OK;
	}

	private CompletableFuture<Void> launchVerticles() {
		CompletableFuture<Void> future = new CompletableFuture<>();
		MQ.init(() -> VertxPlatform.spawnVerticles((e) -> {
			if (!e.succeeded()) {
				future.completeExceptionally(e.cause());
			} else {
				future.complete(null);
			}
		}));
		return future;
	}

	@Override
	public void stop() {
		logger.info("Shutting down.");
	}
}

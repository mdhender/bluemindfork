package net.bluemind.systemd.notify;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.eventbus.DeliveryOptions;
import net.bluemind.lib.vertx.VertxPlatform;

public class Startup {

	private static final String payload = "{}";
	private static final Logger logger = LoggerFactory.getLogger(Startup.class);

	private Startup() {
	}

	/**
	 * Notifies SystemD (if avail) that startup is complete.
	 * 
	 * Do not call multiple times.
	 * 
	 * @return a promise that completes when the systemd notification is done
	 */
	public static CompletableFuture<Void> notifyReady() {
		logger.info("Notifying startup of {}...", System.getProperty("net.bluemind.property.product", "unknown-jvm"));
		CompletableFuture<Void> ret = new CompletableFuture<>();
		VertxPlatform.eventBus().request(NotifyStartupVerticle.ADDR, payload,
				new DeliveryOptions().setSendTimeout(1000), replyStatus -> {
					if (replyStatus.succeeded()) {
						ret.complete(null);
					} else {
						ret.completeExceptionally(replyStatus.cause());
					}
				});
		return ret;
	}

}

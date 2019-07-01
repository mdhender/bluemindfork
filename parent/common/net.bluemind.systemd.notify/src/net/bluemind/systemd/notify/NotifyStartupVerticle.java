package net.bluemind.systemd.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class NotifyStartupVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(NotifyStartupVerticle.class);

	static final String ADDR = "systemd.notify.start";

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new NotifyStartupVerticle();
		}

	}

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		if (SystemD.isAvailable()) {
			Handler<Message<?>> h = (Message<?> msg) -> {
				SystemD.Api api = SystemD.get();
				api.notifyReady();
				msg.reply();
			};
			eb.registerHandler(ADDR, h);
			eb.registerLocalHandler("systemd.notify.unreg", msg -> eb.unregisterHandler(ADDR, h));
		} else {
			logger.warn("SystemD support is missing {}.",
					System.getProperty("net.bluemind.property.product", "unknown-jvm"));
		}
	}

}

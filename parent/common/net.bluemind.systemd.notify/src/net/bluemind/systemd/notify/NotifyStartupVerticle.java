package net.bluemind.systemd.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class NotifyStartupVerticle extends AbstractVerticle {

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
			Handler<Message<Object>> h = (Message<Object> msg) -> {
				SystemD.Api api = SystemD.get();
				api.notifyReady();
				msg.reply(null);
			};
			eb.consumer(ADDR, h);
		} else {
			logger.warn("SystemD support is missing {}.",
					System.getProperty("net.bluemind.property.product", "unknown-jvm"));
		}
	}

}

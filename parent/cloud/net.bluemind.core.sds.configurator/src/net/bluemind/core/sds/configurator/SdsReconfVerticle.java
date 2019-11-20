package net.bluemind.core.sds.configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.sds.proxy.mgmt.SdsProxyManager;

public class SdsReconfVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(SdsReconfVerticle.class);

	public static class SdsReconfFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsReconfVerticle();
		}

	}

	@Override
	public void start() {
		vertx.eventBus().registerHandler("sds.sysconf.changed", (Message<JsonObject> msg) -> {
			JsonObject body = msg.body();

			SdsProxyManager sm = new SdsProxyManager(vertx, body.getString("backend"));
			sm.applyConfiguration(body.getObject("config")).whenComplete((v, ex) -> {
				if (ex != null) {
					logger.error(ex.getMessage(), ex);
				} else {
					msg.reply(true);
					sm.close();
				}
			});
		});
	}
}

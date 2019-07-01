package net.bluemind.directory.service.internal;

import org.apache.commons.lang.StringUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.netflix.spectator.api.Registry;

import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class DirectoryVerticle extends Verticle {
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), DirectoryVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DirectoryVerticle();
		}

	}

	@Override
	public void start() {
		getVertx().eventBus().registerHandler("dir.changed", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				String domain = event.body().getString("domain");
				String version = event.body().getString("version");
				if (containsValidVersion(domain, version)) {
					registry.gauge(idFactory.name("dirVersion", "domainUid", domain, "source", "database"))
							.set(Long.parseLong(version));
				}
			}
		});
	}

	private boolean containsValidVersion(String domain, String version) {
		if (domain == null || domain.equals("")) {
			return false;
		}
		if (!StringUtils.isNumeric(version) || version.equals("")) {
			return false;
		}
		return true;
	}
}

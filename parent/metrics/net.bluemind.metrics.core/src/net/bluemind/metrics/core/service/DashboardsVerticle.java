package net.bluemind.metrics.core.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.core.tick.ITickDashboardProvider;
import net.bluemind.metrics.core.tick.TickDashboards;
import net.bluemind.metrics.core.tick.client.ChronografClient;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class DashboardsVerticle extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(DashboardsVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DashboardsVerticle();
		}

	}

	@Override
	public void start() {
		super.start();
		eb.registerHandler("chronograf.configuration", (Message<JsonObject> msg) -> {
			configureChronograf();
			msg.reply(new JsonObject().putString("status", "ok"));
		});
		eb.registerHandler("metrics.range.annotate", (Message<JsonObject> msg) -> {
			JsonObject js = msg.body();
			String name = js.getString("name");
			Date startDT = new Date(js.getLong("start", System.currentTimeMillis()));
			Date endDT = new Date(js.getLong("end", System.currentTimeMillis()));
			annotateBoard(name, startDT, endDT);
		});
	}

	private void annotateBoard(String name, Date start, Date end) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
		Optional<ItemValue<Server>> chronograf = serverApi.allComplete().stream()
				.filter(iv -> iv.value.tags.contains("metrics/influxdb")).findFirst();
		if (!chronograf.isPresent()) {
			logger.warn("Missing chronograf server");
		} else {
			logger.debug("Chronograf server is {}", chronograf.get().value.address());
			logger.info("Publish annotation {}", name);
			try (ChronografClient chronoClient = new ChronografClient(chronograf.get())) {
				chronoClient.annotate(name, start, end);
			}
		}
	}

	private void configureChronograf() {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
		Optional<ItemValue<Server>> chronograf = serverApi.allComplete().stream()
				.filter(iv -> iv.value.tags.contains("metrics/influxdb")).findFirst();
		if (!chronograf.isPresent()) {
			logger.warn("Missing chronograf server");
			return;
		} else {
			logger.info("Chronograf server is {}", chronograf.get().value.address());
		}
		serverApi.submitAndWait(chronograf.get().uid, "service chronograf restart");
		new NetworkHelper(chronograf.get().value.address()).waitForListeningPort(8888, 10, TimeUnit.SECONDS);

		List<ITickDashboardProvider> dashboards = TickDashboards.dashboards();
		try (ChronografClient chronoClient = new ChronografClient(chronograf.get())) {
			for (ITickDashboardProvider dashProv : dashboards) {
				chronoClient.createOrUpdateDashboard(dashProv);
			}
		}
	}
}

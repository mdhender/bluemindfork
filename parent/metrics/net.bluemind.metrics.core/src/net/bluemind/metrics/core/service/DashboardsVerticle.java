package net.bluemind.metrics.core.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
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
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class DashboardsVerticle extends AbstractVerticle {

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
		EventBus eb = vertx.eventBus();
		eb.consumer("chronograf.configuration", (Message<JsonObject> msg) -> {
			if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
				return;
			}
			configureChronograf();
			msg.reply(new JsonObject().put("status", "ok"));
		});
		eb.consumer("metrics.range.annotate", (Message<JsonObject> msg) -> {
			if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
				return;
			}
			JsonObject js = msg.body();
			String name = js.getString("name");
			Date startDT = new Date(js.getLong("start", System.currentTimeMillis()));
			Date endDT = new Date(js.getLong("end", System.currentTimeMillis()));
			JsonObject tagjs = js.getJsonObject("tags", new JsonObject());
			Map<String, String> flat = new HashMap<>();
			tagjs.forEach(e -> flat.put(e.getKey(), e.getValue().toString()));
			annotateBoard(name, startDT, endDT, flat);
		});
	}

	private void annotateBoard(String name, Date start, Date end, Map<String, String> tags) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
		Optional<ItemValue<Server>> chronograf = serverApi.allComplete().stream()
				.filter(iv -> iv.value.tags.contains(TagDescriptor.bm_metrics_influx.getTag())).findFirst();
		if (!chronograf.isPresent()) {
			logger.warn("Missing chronograf server");
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("Chronograf server is {}", chronograf.get().value.address());
			}
			logger.info("Publish annotation {}", name);
			try (ChronografClient chronoClient = new ChronografClient(chronograf.get())) {
				chronoClient.annotate(name, start, end, tags);
			}
		}
	}

	private void configureChronograf() {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
		Optional<ItemValue<Server>> chronograf = serverApi.allComplete().stream()
				.filter(iv -> iv.value.tags.contains(TagDescriptor.bm_metrics_influx.getTag())).findFirst();
		if (!chronograf.isPresent()) {
			logger.warn("Missing chronograf server");
			return;
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("Chronograf server is {}", chronograf.get().value.address());
			}
		}
		serverApi.submitAndWait(chronograf.get().uid, "service", "chronograf", "restart");
		new NetworkHelper(chronograf.get().value.address()).waitForListeningPort(8888, 30, TimeUnit.SECONDS);

		List<ITickDashboardProvider> dashboards = TickDashboards.dashboards();
		try (ChronografClient chronoClient = new ChronografClient(chronograf.get())) {
			for (ITickDashboardProvider dashProv : dashboards) {
				chronoClient.createOrUpdateDashboard(dashProv);
			}
		}
	}
}

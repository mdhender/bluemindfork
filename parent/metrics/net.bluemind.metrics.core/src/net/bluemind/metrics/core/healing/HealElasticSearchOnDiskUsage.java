package net.bluemind.metrics.core.healing;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.metrics.core.tick.TickTemplateHelper.AlertId;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;

public class HealElasticSearchOnDiskUsage extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(HealElasticSearchOnDiskUsage.class);
	private final Product product = Product.ES;

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new HealElasticSearchOnDiskUsage();
		}
	}

	@Override
	public void start() {
		final EventBus eb = vertx.eventBus();
		eb.consumer("kapacitor.alert", (Message<JsonObject> msg) -> {
			JsonObject obj = msg.body();
			String newLevel = obj.getString("level");
			String idStr = obj.getString("id");
			Optional<AlertId> idOpt = TickTemplateHelper.idFromString(idStr);
			idOpt.ifPresent(alert -> {
				if (!alert.alertSubId.equals("elasticsearch-disk-free")) {
					return;
				}
				if ("CRITICAL".equals(newLevel)) {
					logger.error("ElasticSearch disk usage switched to CRITICAL");
					return;
				}
				ElasticsearchClient esClient = ESearchActivator.getClient();
				if (esClient == null) {
					logger.warn("ElasticSearch client is not available");
					return;
				}

				// Mark all indices writable
				// Warning: cluster can have all indices readonly, but still be GREEN
				logger.info("Marking all indices writable again after high disk usage on {}", alert.datalocation);

				HealthStatus healthStatus;
				try {
					esClient.indices().putSettings(p -> p //
							.index("*").settings(s -> s.blocks(b -> b.readOnlyAllowDelete(false))));
				} catch (ElasticsearchException | IOException e) {
					logger.error("[es][full disk] Failed to mark all indices writable", e);
					return;
				}

				try {
					healthStatus = esClient.cluster().health().status();
				} catch (ElasticsearchException | IOException e) {
					logger.error("[es][full disk] Failed to assess cluster health, restarting", e);
					healthStatus = HealthStatus.Red;
				}

				if (healthStatus == HealthStatus.Red) {
					// Restart elasticsearch
					logger.info("Restarting {} for healing after diskfull on {} (cluster status is RED)", product.name,
							alert.datalocation);
					final ServerSideServiceProvider prov = ServerSideServiceProvider
							.getProvider(SecurityContext.SYSTEM);
					final IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
					CommandStatus status = serverApi.submitAndWait(alert.datalocation, "service", product.name,
							"restart");
					if (!status.successful) {
						logger.error("Unable to restart '{}': {}", product.name, String.join("\n", status.output));
					}
				} else {
					logger.info("CLUSTER STATUS is {}, no need to heal", healthStatus);
				}
			});
		});
	}

}

package net.bluemind.metrics.core.healing;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.metrics.core.tick.TickTemplateHelper.AlertId;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;

public class SafeRestartOnHprofVerticle extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(SafeRestartOnHprofVerticle.class);
	private static final Set<Product> handledProds = EnumSet.of(Product.EAS, Product.LMTPD, Product.MILTER);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SafeRestartOnHprofVerticle();
		}
	}

	@Override
	public void start() {
		super.start();
		eb.registerHandler("kapacitor.alert", (Message<JsonObject> msg) -> {
			JsonObject obj = msg.body();
			String newLevel = obj.getString("level");
			if ("OK".equals(newLevel)) {
				return;
			}
			String idStr = obj.getString("id");
			Optional<AlertId> idOpt = TickTemplateHelper.idFromString(idStr);
			idOpt.ifPresent(id -> {
				if (id.alertSubId.contains("hprof")) {
					for (Product prod : handledProds) {
						if (prod.name.equals(id.product.name)) {
							logger.info("Handling hprof for product {}", prod.name);
							ServerSideServiceProvider prov = ServerSideServiceProvider
									.getProvider(SecurityContext.SYSTEM);
							IServer serverApi = prov.instance(IServer.class, InstallationId.getIdentifier());
							CommandStatus status = serverApi.submitAndWait(id.datalocation,
									"service " + prod.name + " restart");
							logger.info("Handled hprof for {} on {}, {}", prod.name, id.datalocation, status.output);
						}
					}
				}
			});
		});
	}

}

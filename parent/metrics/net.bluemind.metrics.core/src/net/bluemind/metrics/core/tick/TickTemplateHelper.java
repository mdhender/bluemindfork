package net.bluemind.metrics.core.tick;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.metrics.core.Product;
import net.bluemind.server.api.Server;

public class TickTemplateHelper {

	private static final Logger logger = LoggerFactory.getLogger(TickTemplateHelper.class);

	public static class AlertId {
		public final Product product;
		public final String alertSubId;
		public final String datalocation;

		public AlertId(Product p, String alertSubId, String datalocation) {
			this.product = p;
			this.alertSubId = alertSubId;
			this.datalocation = datalocation;
		}
	}

	public static String newId(Product p, String alertSubId, ItemValue<Server> server) {
		if (p.name.contains("_") || server.uid.contains("_")) {
			throw new ServerFault("Invalid id informations");
		}
		return p.name + '_' + alertSubId.replace('_', '-') + '_' + server.uid;
	}

	public static Optional<AlertId> idFromString(String id) {
		String tmp[] = id.split("_");

		Product p = Product.byName(tmp[0]);
		if (tmp.length != 3 || p == null) {
			logger.warn("Invalid alert id {}", id);
			return Optional.empty();
		}
		return Optional.of(new AlertId(p, tmp[1], tmp[2]));
	}
}

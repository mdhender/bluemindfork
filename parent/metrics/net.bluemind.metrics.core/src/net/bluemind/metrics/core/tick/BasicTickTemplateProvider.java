package net.bluemind.metrics.core.tick;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.metrics.core.Product;
import net.bluemind.server.api.Server;

public abstract class BasicTickTemplateProvider implements ITickTemplateProvider {

	protected TemplateDefinition initTickDefinition(ItemValue<Server> server, Product prod, String endPointUrl,
			String defName) {
		TemplateDefinition def = new TemplateDefinition(defName);

		def.variables.put("datalocation", new JsonObject().put("type", "string").put("value", server.uid));
		def.variables.put("bmProduct", new JsonObject().put("type", "string").put("value", prod.name));
		def.variables.put("alertsEndPoint", new JsonObject().put("type", "string").put("value", endPointUrl));
		return def;
	}
}

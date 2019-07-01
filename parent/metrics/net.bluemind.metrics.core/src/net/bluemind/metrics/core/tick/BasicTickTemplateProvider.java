package net.bluemind.metrics.core.tick;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.metrics.core.Product;
import net.bluemind.server.api.Server;

public abstract class BasicTickTemplateProvider implements ITickTemplateProvider {

	protected TemplateDefinition initTickDefinition(ItemValue<Server> server, Product prod, String endPointUrl,
			String defName) {
		TemplateDefinition def = new TemplateDefinition(defName);

		def.variables.putObject("datalocation",
				new JsonObject().putString("type", "string").putString("value", server.uid));
		// def.variables.putObject("host",
		// new JsonObject().putString("type", "string").putString("value",
		// server.value.address()));
		def.variables.putObject("bmProduct",
				new JsonObject().putString("type", "string").putString("value", prod.name));
		def.variables.putObject("alertsEndPoint",
				new JsonObject().putString("type", "string").putString("value", endPointUrl));
		return def;
	}
}

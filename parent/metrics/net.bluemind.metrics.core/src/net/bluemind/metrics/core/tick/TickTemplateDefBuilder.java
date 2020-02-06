package net.bluemind.metrics.core.tick;

import io.vertx.core.json.JsonObject;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.ITickTemplateProvider.TemplateDefinition;

public class TickTemplateDefBuilder {
	private TemplateDefinition def;

	public TickTemplateDefBuilder(String alertId) {
		this.def = new TemplateDefinition(alertId);
		def.variables.put("name", new JsonObject().put("type", "string").put("value", alertId));
	}

	public TickTemplateDefBuilder withDatalocation(String dataloc) {
		def.variables.put("datalocation", new JsonObject().put("type", "string").put("value", dataloc));
		return this;
	}

	public TickTemplateDefBuilder withProduct(Product prod) {
		def.variables.put("bmProduct", new JsonObject().put("type", "string").put("value", prod.name));
		return this;
	}

	public TickTemplateDefBuilder withEndPoint(String alertsEndPoint) {
		def.variables.put("alertsEndPoint", new JsonObject().put("type", "string").put("value", alertsEndPoint));
		return this;
	}

	public TickTemplateDefBuilder withVariable(String varName, int value) {
		def.variables.put(varName, new JsonObject().put("type", "int").put("value", value));
		return this;
	}

	public TickTemplateDefBuilder withVariable(String varName, String value) {
		def.variables.put(varName, new JsonObject().put("type", "string").put("value", value));
		return this;
	}

	public TemplateDefinition build() {
		return this.def;
	}
}

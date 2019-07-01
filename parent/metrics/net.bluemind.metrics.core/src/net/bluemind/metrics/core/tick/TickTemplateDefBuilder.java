package net.bluemind.metrics.core.tick;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.ITickTemplateProvider.TemplateDefinition;

public class TickTemplateDefBuilder {
	private TemplateDefinition def;

	public TickTemplateDefBuilder(String alertId) {
		this.def = new TemplateDefinition(alertId);
		def.variables.putObject("name", new JsonObject().putString("type", "string").putString("value", alertId));
	}

	public TickTemplateDefBuilder withDatalocation(String dataloc) {
		def.variables.putObject("datalocation",
				new JsonObject().putString("type", "string").putString("value", dataloc));
		return this;
	}

	public TickTemplateDefBuilder withProduct(Product prod) {
		def.variables.putObject("bmProduct",
				new JsonObject().putString("type", "string").putString("value", prod.name));
		return this;
	}

	public TickTemplateDefBuilder withEndPoint(String alertsEndPoint) {
		def.variables.putObject("alertsEndPoint",
				new JsonObject().putString("type", "string").putString("value", alertsEndPoint));
		return this;
	}

	public TickTemplateDefBuilder withVariable(String varName, int value) {
		def.variables.putObject(varName, new JsonObject().putString("type", "int").putNumber("value", value));
		return this;
	}

	public TickTemplateDefBuilder withVariable(String varName, String value) {
		def.variables.putObject(varName, new JsonObject().putString("type", "string").putString("value", value));
		return this;
	}

	public TemplateDefinition build() {
		return this.def;
	}
}

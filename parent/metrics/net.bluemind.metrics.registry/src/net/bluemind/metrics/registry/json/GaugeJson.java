package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class GaugeJson extends RegJson {

	private final Double value;

	public GaugeJson(Id id, Double value) {
		super("Gauge", id);
		this.value = value;
	}

	public double getValue() {
		return this.value;
	}
}
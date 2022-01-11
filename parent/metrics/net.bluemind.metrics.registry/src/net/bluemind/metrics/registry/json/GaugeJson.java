package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class GaugeJson extends RegJson {

	private final double value;

	public GaugeJson(Id id, double v) {
		super("Gauge", id);
		this.value = Double.isNaN(v) ? 0 : v;
	}

	public double getValue() {
		return value;
	}
}
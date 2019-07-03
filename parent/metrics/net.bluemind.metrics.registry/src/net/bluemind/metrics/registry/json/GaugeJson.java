package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class GaugeJson extends RegJson {
	private static final String METRICTYPE = "Gauge";
	private Double value;

	public GaugeJson(Id id, Double value) {
		super(METRICTYPE, id);
		this.value = value;
	}

	public GaugeJson(String id, Double value) {
		super(METRICTYPE, id);
		this.value = value;
	}

	public double getValue() {
		return this.value;
	}
}
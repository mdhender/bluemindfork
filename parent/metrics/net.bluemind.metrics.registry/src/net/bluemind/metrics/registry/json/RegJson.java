package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public abstract class RegJson {
	private String metricType;
	private String id;

	protected RegJson(String metricType, Id id) {
		this.metricType = metricType;
		this.id = id.toString();
	}

	protected RegJson(String metricType, String id) {
		this.metricType = metricType;
		this.id = id;
	}

	public String getMetricType() {
		return this.metricType;
	}
	
	public String getId() {
		return this.id;
	}
}

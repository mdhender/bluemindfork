package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class CounterJson extends RegJson {
	private static final String METRICTYPE = "Counter";
	private Long count;

	public CounterJson(Id id, Long count) {
		super(METRICTYPE, id);
		this.count = count;
	}

	public CounterJson(String id, Long count) {
		super(METRICTYPE, id);
		this.count = count;
	}

	public long getCount() {
		return this.count;
	}
}

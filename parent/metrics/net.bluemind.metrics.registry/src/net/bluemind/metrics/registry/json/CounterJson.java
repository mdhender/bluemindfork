package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class CounterJson extends RegJson {
	private final Long count;

	public CounterJson(Id id, Long count) {
		super("Counter", id);
		this.count = count;
	}

	public long getCount() {
		return this.count;
	}
}

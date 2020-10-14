package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class CounterJson extends RegJson {
	private long count;

	public CounterJson(Id id, long count) {
		super("Counter", id);
		this.count = count;
	}

	public long getCount() {
		return this.count;
	}

	public void setCount(long c) {
		this.count = c;
	}

	public CounterJson withCount(long c) {
		setCount(c);
		return this;
	}
}

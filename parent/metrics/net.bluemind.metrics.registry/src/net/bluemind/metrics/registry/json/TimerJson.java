package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class TimerJson extends RegJson {

	private long amount;

	public TimerJson(Id id, long amount) {
		super("Timer", id);
		this.amount = amount;
	}

	public long getAmount() {
		return this.amount;
	}

	public TimerJson withNanos(long ns) {
		this.amount = ns;
		return this;
	}
}

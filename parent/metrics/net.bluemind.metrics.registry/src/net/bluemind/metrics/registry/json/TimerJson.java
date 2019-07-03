package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class TimerJson extends RegJson {
	public static String METRICTYPE = "Timer";
	private final long amount;

	public TimerJson(Id id, long amount) {
		super(METRICTYPE, id);
		this.amount = amount;
	}

	public long getAmount() {
		return this.amount;
	}
}

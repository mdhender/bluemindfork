package net.bluemind.metrics.registry.json;

import com.netflix.spectator.api.Id;

public class DistributionSummaryJson extends RegJson {
	public static String METRICTYPE = "DistributionSummmary";
	private final long amount;

	public DistributionSummaryJson(Id id, long amount) {
		super(METRICTYPE, id);
		this.amount = amount;
	}

	public long getAmount() {
		return this.amount;
	}
}

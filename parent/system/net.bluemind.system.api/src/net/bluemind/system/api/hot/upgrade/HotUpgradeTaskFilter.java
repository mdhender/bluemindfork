package net.bluemind.system.api.hot.upgrade;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class HotUpgradeTaskFilter {
	public List<HotUpgradeTaskStatus> statuses;
	public boolean onlyRetryable;
	public boolean onlyReady;
	public boolean onlyMandatory;
	public List<HotUpgradeTaskExecutionMode> mode;

	private HotUpgradeTaskFilter(List<HotUpgradeTaskStatus> statuses, boolean onlyRetryable, boolean onlyReady,
			boolean onlyMandatory, List<HotUpgradeTaskExecutionMode> mode) {
		this.statuses = Collections.unmodifiableList(statuses);
		this.onlyRetryable = onlyRetryable;
		this.onlyReady = onlyReady;
		this.onlyMandatory = onlyMandatory;
		this.mode = Collections.unmodifiableList(mode);
	}

	private HotUpgradeTaskFilter(List<HotUpgradeTaskStatus> statuses) {
		this(statuses, false, false, false,
				Arrays.asList(HotUpgradeTaskExecutionMode.DIRECT, HotUpgradeTaskExecutionMode.JOB));
	}

	public HotUpgradeTaskFilter() {
		this(Collections.emptyList());
	}

	public List<HotUpgradeTaskStatus> getStatuses() {
		return statuses;
	}

	public boolean onlyRetryable() {
		return onlyRetryable;
	}

	public HotUpgradeTaskFilter onlyRetryable(boolean onlyRetryable) {
		this.onlyRetryable = onlyRetryable;
		return this;
	}

	public boolean onlyReady() {
		return onlyReady;
	}

	public HotUpgradeTaskFilter onlyReady(boolean onlyReady) {
		this.onlyReady = onlyReady;
		return this;
	}

	public HotUpgradeTaskFilter mode(HotUpgradeTaskExecutionMode... mode) {
		this.mode = Arrays.asList(mode);
		return this;
	}

	public boolean onlyMandatory() {
		return onlyMandatory;
	}

	public HotUpgradeTaskFilter onlyMandatory(boolean onlyMandatory) {
		this.onlyMandatory = onlyMandatory;
		return this;
	}

	public static HotUpgradeTaskFilter filter(HotUpgradeTaskStatus... statuses) {
		return new HotUpgradeTaskFilter(Arrays.asList(statuses));
	}

	public static HotUpgradeTaskFilter all() {
		return new HotUpgradeTaskFilter();
	}

	public List<HotUpgradeTaskExecutionMode> getMode() {
		return mode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HotUpgradeTaskFilter [statuses=").append(statuses).append(", onlyRetryable=")
				.append(onlyRetryable).append(", onlyReady=").append(onlyReady).append(", modes=").append(mode)
				.append("]");
		return builder.toString();
	}

}

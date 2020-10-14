package net.bluemind.system.api.hot.upgrade;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class HotUpgradeTaskFilter {

	private final List<HotUpgradeTaskStatus> statuses;
	private final int maxFailure;
	private final List<HotUpgradeTaskExecutionMode> mode;

	private HotUpgradeTaskFilter(List<HotUpgradeTaskStatus> statuses, int maxFailure,
			List<HotUpgradeTaskExecutionMode> mode) {
		this.statuses = Collections.unmodifiableList(statuses);
		this.maxFailure = maxFailure;
		this.mode = Collections.unmodifiableList(mode);
	}

	private HotUpgradeTaskFilter(List<HotUpgradeTaskStatus> statuses) {
		this(statuses, -1, Arrays.asList(HotUpgradeTaskExecutionMode.DIRECT, HotUpgradeTaskExecutionMode.JOB));
	}

	public HotUpgradeTaskFilter() {
		this(Collections.emptyList());
	}

	public List<HotUpgradeTaskStatus> getStatuses() {
		return statuses;
	}

	public int getMaxFailure() {
		return maxFailure;
	}

	public HotUpgradeTaskFilter withMaxFailure(int maxFailure) {
		return new HotUpgradeTaskFilter(statuses, maxFailure, mode);
	}

	public HotUpgradeTaskFilter mode(HotUpgradeTaskExecutionMode... mode) {
		return new HotUpgradeTaskFilter(statuses, maxFailure, Arrays.asList(mode));
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
		builder.append("HotUpgradeTaskFilter [statuses=").append(statuses).append(", maxFailure=").append(maxFailure)
				.append(", modes=").append(mode).append("]");
		return builder.toString();
	}

}

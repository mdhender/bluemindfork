package net.bluemind.system.api.hot.upgrade;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class HotUpgradeProgress {
	public HotUpgradeTaskStatus status;
	public long count = 0;

	public Date lastUpdatedAt;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HotUpgradeProgress [status=").append(status).append(", count=").append(count)
				.append(", lastUpdatedAt=").append(lastUpdatedAt).append("]");
		return builder.toString();
	}

}

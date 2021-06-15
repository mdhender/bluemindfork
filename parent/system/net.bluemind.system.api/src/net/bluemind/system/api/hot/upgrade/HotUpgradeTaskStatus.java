package net.bluemind.system.api.hot.upgrade;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum HotUpgradeTaskStatus {
	SUCCESS, FAILURE, PLANNED
}

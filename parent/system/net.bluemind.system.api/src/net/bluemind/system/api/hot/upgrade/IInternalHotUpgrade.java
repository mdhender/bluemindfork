package net.bluemind.system.api.hot.upgrade;

import net.bluemind.core.task.api.TaskRef;

public interface IInternalHotUpgrade extends IHotUpgrade {

	/**
	 * Create an {@link HotUpgradeTask}
	 * 
	 * HotUpgradeTask status, createdAt and updatedAt properties are optional. If
	 * status is null, it is set to {@link HotUpgradeTaskStatus#PLANNED} If
	 * createdAt and updatedAt are null, they are set to the current timestamp.
	 * 
	 * @param task
	 */
	void create(HotUpgradeTask task);

	/**
	 * Update {@link HotUpgradeTask} status, failure and updatedAt properties
	 * 
	 * @param task
	 */
	void update(HotUpgradeTask task);

	TaskRef start();
}

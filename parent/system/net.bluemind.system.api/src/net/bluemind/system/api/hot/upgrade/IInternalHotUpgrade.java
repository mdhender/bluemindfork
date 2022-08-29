package net.bluemind.system.api.hot.upgrade;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

@BMApi(version = "3", internal = true)
@Path("/hot_upgrade")
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

	@POST
	@Path("start")
	TaskRef start(@QueryParam(value = "onlyReady") boolean onlyReady, HotUpgradeTaskExecutionMode mode);

	@POST
	@Path("limitedStart")
	TaskRef startLimited(@QueryParam(value = "maxDuration") long maxDuration, HotUpgradeTaskExecutionMode mode);

}

package net.bluemind.maintenance;

import net.bluemind.core.task.service.IServerTaskMonitor;

public interface IMaintenanceScript {
	public void run(IServerTaskMonitor monitor);
}

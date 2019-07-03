package net.bluemind.metrics.core.tick;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class TickDashboards {
	public static List<ITickDashboardProvider> dashboards() {
		RunnableExtensionLoader<ITickDashboardProvider> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensions("net.bluemind.metrics.core", "tickDashboardProvider", "tick_dashboard_provider",
				"impl");
	}
}

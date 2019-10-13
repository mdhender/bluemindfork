package net.bluemind.forest.cloud.launcher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ForestLauncherActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		ForestLauncherActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		ForestLauncherActivator.context = null;
	}

}

package net.bluemind.sds.proxy.launcher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SdsLauncherActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		SdsLauncherActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		SdsLauncherActivator.context = null;
	}

}

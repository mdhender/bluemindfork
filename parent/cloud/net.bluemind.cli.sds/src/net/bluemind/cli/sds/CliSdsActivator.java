package net.bluemind.cli.sds;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CliSdsActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		CliSdsActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		CliSdsActivator.context = null;
	}

}

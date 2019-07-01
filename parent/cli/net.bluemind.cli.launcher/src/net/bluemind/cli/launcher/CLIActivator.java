package net.bluemind.cli.launcher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CLIActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		CLIActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		CLIActivator.context = null;
	}

}

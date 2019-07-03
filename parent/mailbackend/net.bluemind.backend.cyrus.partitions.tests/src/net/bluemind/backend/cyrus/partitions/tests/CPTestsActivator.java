package net.bluemind.backend.cyrus.partitions.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CPTestsActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		CPTestsActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		CPTestsActivator.context = null;
	}

}

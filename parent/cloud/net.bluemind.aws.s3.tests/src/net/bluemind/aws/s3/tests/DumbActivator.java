package net.bluemind.aws.s3.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DumbActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		DumbActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		DumbActivator.context = null;
	}

}

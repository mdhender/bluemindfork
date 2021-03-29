package net.bluemind.sds.store.scalityring.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ScalityRingTestsActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		ScalityRingTestsActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		ScalityRingTestsActivator.context = null;
	}

}

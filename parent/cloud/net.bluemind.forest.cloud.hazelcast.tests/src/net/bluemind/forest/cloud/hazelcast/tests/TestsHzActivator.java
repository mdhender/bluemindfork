package net.bluemind.forest.cloud.hazelcast.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TestsHzActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		TestsHzActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		TestsHzActivator.context = null;
	}

}

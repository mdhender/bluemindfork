package net.bluemind.forest.cloud.hazelcast;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class HzCloudActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		HzCloudActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		HzCloudActivator.context = null;
	}

}

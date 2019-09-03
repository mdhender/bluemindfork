package net.bluemind.sds.proxy;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SdsProxyActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		SdsProxyActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		SdsProxyActivator.context = null;
	}

}

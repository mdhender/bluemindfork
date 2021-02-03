package net.bluemind.lmtp.filter.tnef;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TnefFilterActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		TnefFilterActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		TnefFilterActivator.context = null;
	}

}

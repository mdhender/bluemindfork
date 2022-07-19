package net.bluemind.sds.store.cyrusspool;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CyrusStoreActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		CyrusStoreActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		CyrusStoreActivator.context = null;
	}

}

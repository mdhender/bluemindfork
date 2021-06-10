package net.bluemind.core.backup.continuous.clone;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CloneActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		CloneActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		CloneActivator.context = null;
	}

}

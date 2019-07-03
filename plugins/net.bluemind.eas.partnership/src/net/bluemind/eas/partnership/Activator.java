package net.bluemind.eas.partnership;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		Provider.classLoad();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}

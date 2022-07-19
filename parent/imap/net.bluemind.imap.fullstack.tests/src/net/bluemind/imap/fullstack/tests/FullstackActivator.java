package net.bluemind.imap.fullstack.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FullstackActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		FullstackActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		FullstackActivator.context = null;
	}

}

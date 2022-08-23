package net.bluemind.imap.cyrus.storage.testhelper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TestHelperActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		TestHelperActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		TestHelperActivator.context = null;
	}

}

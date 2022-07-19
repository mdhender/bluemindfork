package net.bluemind.backend.mailapi.storage;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MailApiStorageActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		MailApiStorageActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		MailApiStorageActivator.context = null;
	}

}

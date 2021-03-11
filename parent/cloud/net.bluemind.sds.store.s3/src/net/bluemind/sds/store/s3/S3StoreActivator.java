package net.bluemind.sds.store.s3;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class S3StoreActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		S3StoreActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		S3StoreActivator.context = null;
	}

}

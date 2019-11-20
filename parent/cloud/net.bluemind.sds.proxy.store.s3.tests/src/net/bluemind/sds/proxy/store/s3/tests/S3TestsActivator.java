package net.bluemind.sds.proxy.store.s3.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class S3TestsActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		S3TestsActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		S3TestsActivator.context = null;
	}

}

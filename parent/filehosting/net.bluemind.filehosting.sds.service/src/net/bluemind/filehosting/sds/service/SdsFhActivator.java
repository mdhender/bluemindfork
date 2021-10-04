package net.bluemind.filehosting.sds.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class SdsFhActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		SdsFhActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		SdsFhActivator.context = null;
	}

}

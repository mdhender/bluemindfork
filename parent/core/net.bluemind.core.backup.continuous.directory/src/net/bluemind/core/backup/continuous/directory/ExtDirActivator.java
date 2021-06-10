package net.bluemind.core.backup.continuous.directory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ExtDirActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		ExtDirActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		ExtDirActivator.context = null;
	}

}

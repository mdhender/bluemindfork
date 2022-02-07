package net.bluemind.document.persistence.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private static Path path;

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		path = Paths.get("/var/spool/bm-docs");
		path.toFile().mkdirs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	public static Path getStorageDir() {
		return path;
	}
}

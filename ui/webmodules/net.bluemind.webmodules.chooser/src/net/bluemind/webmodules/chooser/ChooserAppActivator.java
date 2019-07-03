package net.bluemind.webmodules.chooser;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ChooserAppActivator implements BundleActivator {

	public static String version;

	@Override
	public void start(BundleContext ctx) throws Exception {
		version = ctx.getBundle().getVersion().toString();
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {

	}

}

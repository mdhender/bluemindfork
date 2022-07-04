package net.bluemind.imap.endpoint;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ImapEndpointActivator implements BundleActivator {

	private static BundleContext context;
	private static String version;

	public static synchronized BundleContext getContext() {
		return context;
	}

	public static synchronized String getVersion() {
		return version;
	}

	private static synchronized void setContext(BundleContext ctx) {
		context = ctx;
	}

	private static synchronized void setVersion(String v) {
		version = v;
	}

	public void start(BundleContext bundleContext) throws Exception {
		setContext(bundleContext);
		setVersion(context.getBundle().getVersion().toString());
	}

	public void stop(BundleContext bundleContext) throws Exception {
		setContext(null);
	}

}

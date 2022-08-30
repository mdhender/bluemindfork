package net.bluemind.delivery.lmtp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MailApiLmtpActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		MailApiLmtpActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		MailApiLmtpActivator.context = null;
	}

}

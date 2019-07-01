package net.bluemind.eas.backend.bm;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eas.partnership.Provider;

public class BackendActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		// ensure partnership stuff is loaded before vertx classloader kicks in
		Provider.get();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// do nothing
	}

}

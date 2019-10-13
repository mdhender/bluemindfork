package net.bluemind.kafka.client.tests;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class KafkaActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		KafkaActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		KafkaActivator.context = null;
	}

}

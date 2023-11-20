package net.bluemind.elastic.topology.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class EsTopologyActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		EsTopologyActivator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		EsTopologyActivator.context = null;
	}

}

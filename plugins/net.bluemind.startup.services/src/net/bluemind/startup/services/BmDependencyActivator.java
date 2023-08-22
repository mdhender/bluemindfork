package net.bluemind.startup.services;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.BundleContext;

import net.bluemind.startup.services.impl.BmServiceDependency;
import net.bluemind.startup.services.impl.ServiceDependenciesLookup;

public abstract class BmDependencyActivator extends DependencyActivatorBase {

	public static boolean running;

	@Override
	public void start(BundleContext context) throws Exception {
		ServiceDependenciesLookup.bundleStarting.put(context.getBundle(), true);
		super.start(context);
		ServiceDependenciesLookup.bundleStarting.remove(context.getBundle());
	}

	@Override
	public ServiceDependency createServiceDependency() {
		return new BmServiceDependency(getBundleContext(), super.createServiceDependency());
	}
}

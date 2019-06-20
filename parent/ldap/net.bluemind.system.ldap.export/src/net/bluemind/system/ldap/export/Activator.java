package net.bluemind.system.ldap.export;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.ldap.export.enhancer.IEntityEnhancer;

public class Activator implements BundleActivator {
	private static final List<IEntityEnhancer> entityEnhancerHooks = loadEntityEnhancerHooks();

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	private static List<IEntityEnhancer> loadEntityEnhancerHooks() {
		RunnableExtensionLoader<IEntityEnhancer> loader = new RunnableExtensionLoader<IEntityEnhancer>();
		return loader.loadExtensionsWithPriority("net.bluemind.system.ldap.export", "entityenhancer", "hook", "impl");
	}

	public static List<IEntityEnhancer> getEntityEnhancerHooks() {
		return entityEnhancerHooks;
	}
}

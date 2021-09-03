package net.bluemind.role.hook;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class RoleHookActivator implements BundleActivator {

	private static BundleContext context;
	private static List<IRoleHook> roleHooks;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		RoleHookActivator.context = bundleContext;
		RunnableExtensionLoader<IRoleHook> loader = new RunnableExtensionLoader<>();
		roleHooks = loader.loadExtensions("net.bluemind.role.hook", "rolehook", "hook", "impl");
	}

	public void stop(BundleContext bundleContext) throws Exception {
		RoleHookActivator.context = null;
	}

	static List<IRoleHook> getHooks() {
		return roleHooks;
	}
}
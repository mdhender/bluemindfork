/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.proxy.http;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Activator implements BundleActivator {

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	private static ConcurrentHashMap<String, IAuthProviderFactory> authProviders;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		authProviders = new ConcurrentHashMap<String, IAuthProviderFactory>();
		RunnableExtensionLoader<IAuthProviderFactory> rel = new RunnableExtensionLoader<IAuthProviderFactory>();
		List<IAuthProviderFactory> aps = rel.loadExtensions("net.bluemind.proxy.http", "authprovider", "auth_provider",
				"implementation");
		for (IAuthProviderFactory apf : aps) {
			authProviders.put(apf.getKind(), apf);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	public static IAuthProviderFactory getAuthProvider(String requiredAuthKind) {
		return authProviders.get(requiredAuthKind);
	}

	public static void registerSessionListener(ILogoutListener listener) {
		for (IAuthProviderFactory apf : authProviders.values()) {
			apf.setLogoutListener(listener);
		}
	}
}

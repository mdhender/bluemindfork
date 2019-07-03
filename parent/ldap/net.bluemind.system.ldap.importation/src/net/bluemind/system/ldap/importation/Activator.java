package net.bluemind.system.ldap.importation;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.enhancer.IScannerEnhancer;
import net.bluemind.system.importation.commons.pool.LdapPoolByDomain;

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

public class Activator implements BundleActivator {
	private static LdapPoolByDomain poolByDomain = new LdapPoolByDomain();
	private static final List<IEntityEnhancer> entityEnhancerHooks = loadEntityEnhancerHooks();
	private static final List<IScannerEnhancer> scannerEnhancerHooks = loadScannerEnhancerHooks();

	@Override
	public void start(BundleContext context) throws Exception {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

	private static List<IEntityEnhancer> loadEntityEnhancerHooks() {
		RunnableExtensionLoader<IEntityEnhancer> loader = new RunnableExtensionLoader<IEntityEnhancer>();
		return loader.loadExtensionsWithPriority("net.bluemind.system.ldap.importation", "entityenhancer", "hook",
				"impl");
	}

	private static List<IScannerEnhancer> loadScannerEnhancerHooks() {
		RunnableExtensionLoader<IScannerEnhancer> loader = new RunnableExtensionLoader<IScannerEnhancer>();
		return loader.loadExtensionsWithPriority("net.bluemind.system.ldap.importation", "scannerenhancer", "hook",
				"impl");
	}

	public static List<IEntityEnhancer> getEntityEnhancerHooks() {
		return entityEnhancerHooks;
	}

	public static List<IScannerEnhancer> getScannerEnhancerHooks() {
		return scannerEnhancerHooks;
	}

	public static LdapPoolByDomain getLdapPoolByDomain() {
		return poolByDomain;
	}
}

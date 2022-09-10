/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */

package net.bluemind.startup.dropins;

import java.nio.file.Path;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DropinsActivator implements BundleActivator {
	public static final String BUNDLE_INFOS_LOCATION = "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		String productVersion = bundleContext.getBundle().getVersion().toString();
		String productName = System.getProperty("net.bluemind.property.product");
		Path productPath = Path.of("/usr/share", productName);

		Repository extensions = Repository.create(productPath, "extensions", null);
		Repository dropins = Repository.create(productPath, "dropins", productVersion);

		Path bundlesInfoPath = productPath.resolve(BUNDLE_INFOS_LOCATION);
		BundlesInfoRewriter.rewriteBundlesInfo(bundlesInfoPath, extensions, dropins);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
	}
}

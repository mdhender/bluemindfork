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
package net.bluemind.webmodule.server;

import jakarta.activation.MimetypesFileTypeMap;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class WebModuleServerActivator implements BundleActivator {

	private static List<WebModuleBuilder> modules;

	public static MimetypesFileTypeMap mimeTypes;

	@Override
	public void start(BundleContext context) throws Exception {
		WebModuleResolver resolver = new WebModuleResolver();
		modules = resolver.loadExtensions();
		resolver.logModules(modules);

		URL url = WebModuleServerActivator.class.getResource("mime.types");
		try (InputStream in = url.openStream()) {
			mimeTypes = new MimetypesFileTypeMap(in);
		}
		new WebServerFilters().loadExtensions();
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

	public static List<WebModuleBuilder> getModules() {
		return modules;
	}

	public static List<IWebFilter> getFilters() {
		return new WebServerFilters().loadExtensions();
	}
}

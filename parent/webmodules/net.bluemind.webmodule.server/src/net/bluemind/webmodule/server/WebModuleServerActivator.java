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

import java.util.List;
import java.util.function.Supplier;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.google.common.base.Suppliers;

import jakarta.activation.MimetypesFileTypeMap;
import net.bluemind.webmodule.server.forward.ConfigBuilder;

public class WebModuleServerActivator implements BundleActivator {

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	private static List<WebModuleBuilder> modules;
	private static Supplier<WebserverConfiguration> conf;

	private static MimetypesFileTypeMap mimeTypes;

	@Override
	public void start(BundleContext context) throws Exception {
		WebModuleServerActivator.context = context;
		WebModuleResolver resolver = new WebModuleResolver();
		modules = resolver.loadExtensions();
		resolver.logModules(modules);
		new WebServerFilters().loadExtensions();

		conf = Suppliers.memoize(ConfigBuilder::build);
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

	public static Supplier<WebserverConfiguration> getConf() {
		return conf;
	}
}

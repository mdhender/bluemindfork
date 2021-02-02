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
package net.bluemind.core.rest;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.internal.BmContextImpl;

/**
 * Server-side service instantation
 *
 */
public class ServerSideServiceProvider implements IServiceProvider {

	public static DataSource defaultDataSource = null;
	public static Map<String, DataSource> mailboxDataSource = new HashMap<>();

	public interface IServerSideServiceFactory<T> {
		public Class<T> factoryClass();

		public T instance(BmContext context, String... params) throws ServerFault;
	}

	private Map<Class<?>, IServerSideServiceFactory<?>> factories;
	private BmContext context;

	public ServerSideServiceProvider(BmContext context, Map<Class<?>, IServerSideServiceFactory<?>> factories) {
		this.context = context;
		this.factories = factories;
	}

	@Override
	public <T> T instance(Class<T> interfaceClass, String... params) throws ServerFault {
		@SuppressWarnings("unchecked")
		IServerSideServiceFactory<T> factory = (IServerSideServiceFactory<T>) factories.get(interfaceClass);
		if (factory == null) {
			throw new ServerFault("No factory for " + interfaceClass);
		}
		return factory.instance(context, params);
	}

	public static ServerSideServiceProvider getProvider(SecurityContext sec) {
		BmContext context = new BmContextImpl(sec, defaultDataSource, mailboxDataSource);
		return new ServerSideServiceProvider(context, ServerSideServiceFactories.getInstance().getFactories());
	}

	public static IServiceProvider getProvider(BmContext context) {
		return new ServerSideServiceProvider(context, ServerSideServiceFactories.getInstance().getFactories());
	}

	public BmContext getContext() {
		return context;
	}
}

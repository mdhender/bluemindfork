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
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import net.bluemind.core.api.BMAsyncApi;
import net.bluemind.core.api.BMPromiseApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.base.DirectClientProxy;
import net.bluemind.core.rest.internal.BmContextImpl;

/**
 * Server-side service instantation
 *
 */
public class PromiseServiceProvider implements IServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(PromiseServiceProvider.class);
	public static DataSource defaultDataSource = null;
	public static Map<String, DataSource> mailboxDataSource = new HashMap<String, DataSource>();

	private final BmContext context;
	private final Vertx vertx;

	private static final Map<Class<?>, DirectClientProxy<?, ?>> proxyCache = new ConcurrentHashMap<>();

	public PromiseServiceProvider(Vertx v, BmContext context) {
		this.context = context;
		this.vertx = v;
	}

	@Override
	public <T> T instance(Class<T> interfaceClass, String... params) throws ServerFault {
		DirectClientProxy<?, ?> clientProxy = proxyCache.computeIfAbsent(interfaceClass, iface -> {
			BMPromiseApi promApi = interfaceClass.getAnnotation(BMPromiseApi.class);
			if (promApi == null) {
				throw new ServerFault("Not a promise api " + interfaceClass);
			}
			Class<?> asyncApiClass = promApi.value();
			logger.debug("asyncApiClass: {}", asyncApiClass);
			BMAsyncApi syncApi = asyncApiClass.getAnnotation(BMAsyncApi.class);
			logger.debug("Got asyncApi annot: {}", syncApi);
			Class<?> syncInterfaceClass = syncApi.value();
			logger.debug("SyncAPI class is {} for {}", syncInterfaceClass, interfaceClass);
			return DirectClientProxy.create(vertx, syncInterfaceClass, asyncApiClass);
		});

		@SuppressWarnings("unchecked")
		T fromDirectProxy = (T) clientProxy.client(context.getSecurityContext(), params);

		return PromiseProxy.wrap(interfaceClass, fromDirectProxy);
	}

	public static PromiseServiceProvider getProvider(Vertx v, SecurityContext sec) {
		BmContext context = new BmContextImpl(sec, defaultDataSource, mailboxDataSource);
		return new PromiseServiceProvider(v, context);
	}

	public static IServiceProvider getProvider(Vertx v, BmContext context) {
		return new PromiseServiceProvider(v, context);
	}

	public BmContext getContext() {
		return context;
	}
}

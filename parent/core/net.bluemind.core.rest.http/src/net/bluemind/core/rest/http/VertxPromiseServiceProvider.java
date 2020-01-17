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
package net.bluemind.core.rest.http;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.BMPromiseApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.common.PromiseInvocationHandler;

public class VertxPromiseServiceProvider extends VertxServiceProvider implements IServiceProvider {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(VertxPromiseServiceProvider.class);

	public VertxPromiseServiceProvider(HttpClientProvider httpClientProvider, ILocator locator, String apiKey) {
		this(httpClientProvider, locator, apiKey, Collections.emptyList());
	}

	public VertxPromiseServiceProvider from(HttpServerRequest req) {
		super.from(req);
		return this;

	}

	public VertxPromiseServiceProvider(HttpClientProvider httpClientProvider, ILocator locator, String apiKey,
			List<String> remoteIps) {
		super(httpClientProvider, locator, apiKey, remoteIps);
	}

	@Override
	public <A> A instance(String tag, Class<A> interfaceClass, String... params) {
		BMPromiseApi promiseAnnotation = interfaceClass.getAnnotation(BMPromiseApi.class);
		Objects.requireNonNull(promiseAnnotation,
				"Interface class " + interfaceClass + " is lacking a BMPromiseApi annotation");
		Class<?> asyncApi = promiseAnnotation.value();
		Object asyncImpl = super.instance(tag, asyncApi, params);
		Object promiseProxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] { interfaceClass },
				new PromiseInvocationHandler(asyncImpl));
		return interfaceClass.cast(promiseProxy);
	}

	@Override
	public <T> T instance(Class<T> interfaceClass, String... params) throws ServerFault {
		return instance("bm/core", interfaceClass, params);
	}

}

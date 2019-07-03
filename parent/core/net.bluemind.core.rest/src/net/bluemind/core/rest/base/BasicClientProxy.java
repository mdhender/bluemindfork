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
package net.bluemind.core.rest.base;

import java.util.LinkedList;
import java.util.List;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpHeaders;

import net.bluemind.core.api.BMVersion;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.internal.LocalIP;

public class BasicClientProxy<S, T> {

	private static final CharSequence HEADER_API_KEY = HttpHeaders.createOptimized("X-BM-ApiKey");
	private static final CharSequence HEADER_CLIENT_VERSION = HttpHeaders.createOptimized("X-BM-ClientVersion");
	private static final CharSequence HEADER_VERSION_VALUE = HttpHeaders.createOptimized(BMVersion.getVersion());

	private ClientProxyGenerator<S, T> cpg;
	private IRestCallHandler callHandler;
	private LinkedList<String> remoteAddresses = new LinkedList<>();
	private String origin;

	public BasicClientProxy(IRestCallHandler callHander, Class<S> api, Class<T> asyncApi) {
		this.callHandler = callHander;
		this.cpg = ClientProxyGenerator.generator(api, asyncApi);
		LocalIP.VALUE.ifPresent(remoteAddresses::add);
	}

	public BasicClientProxy<S, T> setRemoteIps(List<String> ips) {
		this.remoteAddresses.addAll(ips);
		return this;
	}

	public BasicClientProxy<S, T> setOrigin(String origin) {
		this.origin = origin;
		return this;
	}

	public BasicClientProxy<S, T> setRemoteIp(String ip) {
		this.remoteAddresses.add(ip);
		return this;
	}

	public T client(final SecurityContext sc, final String... pathParams) {
		return cpg.client(origin, remoteAddresses, callHandler, defaultHeaders(sc.getSessionId()), pathParams);
	}

	public S syncClient(final SecurityContext sc, final String... pathParams) {
		return cpg.syncClient(origin, remoteAddresses, callHandler, defaultHeaders(sc.getSessionId()), pathParams);
	}

	public T client(String sessionId, String... pathParams) {
		return cpg.client(origin, remoteAddresses, callHandler, defaultHeaders(sessionId), pathParams);
	}

	public S syncClient(String sessionId, String... pathParams) {
		return cpg.syncClient(origin, remoteAddresses, callHandler, defaultHeaders(sessionId), pathParams);
	}

	private MultiMap defaultHeaders(String sessionId) {
		MultiMap headers = RestHeaders.newMultimap();
		if (sessionId != null) {
			headers.add(HEADER_API_KEY, sessionId);
		}

		if (BMVersion.getVersion() != null) {
			headers.add(HEADER_CLIENT_VERSION, HEADER_VERSION_VALUE);
		}
		return headers;
	}
}

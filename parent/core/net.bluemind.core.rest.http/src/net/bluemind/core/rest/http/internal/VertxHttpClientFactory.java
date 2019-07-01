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
package net.bluemind.core.rest.http.internal;

import org.vertx.java.core.http.HttpClient;

import net.bluemind.core.rest.base.BasicClientProxy;

public class VertxHttpClientFactory<S, T> extends BasicClientProxy<S, T> {
	private String baseUri;

	public VertxHttpClientFactory(Class<S> api, Class<T> asyncApi, String baseUri, HttpClient client) {
		super(new VertxHttpCallHandler(client, baseUri), api, asyncApi);
	}
}

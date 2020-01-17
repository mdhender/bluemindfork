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
package net.bluemind.xivo.client.impl;

import javax.net.ssl.SSLException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

public final class AHCHelper {

	private static AsyncHttpClient client;

	static {
		AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(false)
				.setMaxRedirects(0).setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0).setRequestTimeout(30000)
				.setSslContext(trust()).build();

		client = new DefaultAsyncHttpClient(config);
	}

	public static AsyncHttpClient get() {
		return client;
	}

	private static SslContext trust() {
		try {
			return SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} catch (SSLException e) {
			throw new RuntimeException(e);// NOSONAR
		}
	}

}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.proxy.support;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import net.bluemind.system.api.SystemConf;

public class AHCWithProxy {
	private static final int TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
	private static final int DEFAULT_IDLE_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
	private static final int MAX_REDIRECT = 5;

	public static DefaultAsyncHttpClientConfig.Builder defaultConfig() {
		return new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(true).setMaxRedirects(MAX_REDIRECT)
				.setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0).setRequestTimeout(TIMEOUT)
				.setReadTimeout(DEFAULT_IDLE_TIMEOUT);
	}

	public static AsyncHttpClient build(SystemConf systemConf) {
		return build(defaultConfig(), systemConf);
	}

	public static AsyncHttpClient build(DefaultAsyncHttpClientConfig.Builder config, SystemConf systemConf) {
		return new DefaultAsyncHttpClient(config.setProxyServerSelector(new BMProxyServerSelector(systemConf)).build());
	}
}

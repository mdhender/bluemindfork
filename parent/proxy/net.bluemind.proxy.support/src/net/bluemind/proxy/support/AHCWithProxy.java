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

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import net.bluemind.system.api.SystemConf;

public class AHCWithProxy {
	private static final int TIMEOUT = 30000;
	private static final int DEFAULT_IDLE_TIMEOUT = 30000;
	private static final int MAX_REDIRECT = 5;

	public static AsyncHttpClient build(SystemConf systemConf) {
		AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(true)
				.setMaxRedirects(MAX_REDIRECT).setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0)
				.setRequestTimeout(TIMEOUT).setReadTimeout(DEFAULT_IDLE_TIMEOUT)
				.setProxyServerSelector(new BMProxyServerSelector(systemConf)).build();
		return new DefaultAsyncHttpClient(config);
	}
}

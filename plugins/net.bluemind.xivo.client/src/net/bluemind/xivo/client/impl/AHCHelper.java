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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import net.bluemind.utils.Trust;

public final class AHCHelper {

	private static AsyncHttpClient client;

	static {
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setFollowRedirect(false).setMaxRedirects(0)
				.setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0).setRequestTimeout(30000)
				.setSSLContext(Trust.createSSLContext()).setAllowPoolingConnections(true).build();
		client = new AsyncHttpClient(config);
	}

	public static AsyncHttpClient get() {
		return client;
	}

}

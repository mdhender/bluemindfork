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
package net.bluemind.node.client.impl;

import com.ning.http.client.AsyncHttpClient;

public class HostPortClient {

	private String host;
	private int port;
	private AsyncHttpClient client;
	private WebsocketLink websocketLink;

	public HostPortClient() {
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public AsyncHttpClient getClient() {
		return client;
	}

	public void setClient(AsyncHttpClient client) {
		this.client = client;
	}

	public boolean isSSL() {
		return 8022 == port;
	}

	public StringBuilder path() {
		StringBuilder sb = new StringBuilder(64);
		sb.append(isSSL() ? "https://" : "http://");
		sb.append(host).append(':').append(port);
		return sb;
	}

	public WebsocketLink getWebsocketLink() {
		return websocketLink;
	}

	public void setWebsocketLink(WebsocketLink websocketLink) {
		this.websocketLink = websocketLink;
	}

}

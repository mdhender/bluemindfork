/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.node.server;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import net.bluemind.lib.vertx.RouteMatcher;

public class BlueMindSSLNode extends BlueMindNode {

	public static final File serverJks = new File("/etc/bm/bm.jks");
	public static final File trustClientCert = new File("/etc/bm/nodeclient_truststore.jks");

	@Override
	protected int getPort() {
		return 8022;
	}

	@Override
	protected void options(HttpServerOptions options) {
		options.setKeyStoreOptions(new JksOptions().setPath("/etc/bm/bm.jks").setPassword("bluemind"));
		options.setSsl(true);
		options.setEnabledSecureTransportProtocols(new HashSet<>(Arrays.asList("TLSv1.2", "TLSv1.3")));
		options.setTrustStoreOptions(
				new JksOptions().setPath("/etc/bm/nodeclient_truststore.jks").setPassword("password"));
		options.setClientAuth(ClientAuth.REQUIRED);
		options.setOpenSslEngineOptions(new OpenSSLEngineOptions());
		logger.info("Configured in secure mode");
	}

	@Override
	protected void router(RouteMatcher rm) {
		rm.options("/ping", (HttpServerRequest event) -> event.response().end());
	}

	public static boolean canSSL() {
		return BlueMindSSLNode.serverJks.exists() && BlueMindSSLNode.trustClientCert.exists();
	}

}

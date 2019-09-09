/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.proxy.mgmt;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

public class SdsProxyManager implements Closeable {

	private final HttpClient client;

	public SdsProxyManager(Vertx vertx, String sdsProxyAddress) {
		this.client = vertx.createHttpClient().setHost(sdsProxyAddress).setPort(8091);
	}

	public CompletableFuture<Void> applyConfiguration(JsonObject js) {
		CompletableFuture<Void> done = new CompletableFuture<>();
		client.post("/configuration", resp -> {
			resp.exceptionHandler(t -> done.completeExceptionally(t));
			resp.endHandler(v -> {
				if (resp.statusCode() == 200) {
					done.complete(null);
				} else {
					done.completeExceptionally(new Exception(resp.statusMessage() + " (" + resp.statusCode() + ")"));
				}
			});
		}).setChunked(true).write(js.encode()).end();

		return done;

	}

	@Override
	public void close() {
		client.close();
	}

}

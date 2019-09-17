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
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Context;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class SdsProxyManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(SdsProxyManager.class);

	private final AsyncHttpClient client;
	private final String confUrl;
	private final Executor contextExecutor;

	/**
	 * Client to manage sds-proxy on a remote server
	 * 
	 * 
	 * @param vertx           callbacks will run in this vertx context
	 * @param sdsProxyAddress ip address or FQDN of the target sds-proxy
	 */
	public SdsProxyManager(Vertx vertx, String sdsProxyAddress) {
		client = new AsyncHttpClient();
		this.confUrl = "http://" + sdsProxyAddress + ":8091/configuration";
		final Context context = vertx.currentContext();
		if (context != null) {
			this.contextExecutor = command -> {
				context.runOnContext(theVoid -> {
					command.run();
				});
			};
		} else {
			this.contextExecutor = cmd -> cmd.run();
		}

	}

	/**
	 * @param js
	 * @return a promise, dependent will run in vertx context
	 */
	public CompletableFuture<Void> applyConfiguration(JsonObject js) {
		BoundRequestBuilder post = client.preparePost(confUrl);
		CompletableFuture<Void> done = new CompletableFuture<>();
		ListenableFuture<Response> futureResp = post.setHeader("Content-Type", "application/json")
				.setBody(js.encode().getBytes()).execute();
		futureResp.addListener(() -> {
			try {
				Response resp = futureResp.get();
				int status = resp.getStatusCode();
				if (status == 200) {
					logger.info("Configure completed for {}", confUrl);
					done.complete(null);
				} else {
					done.completeExceptionally(
							new Exception("Configure call failed: " + status + " " + resp.getStatusText()));
				}
			} catch (Exception e) {
				done.completeExceptionally(e);
			}
		}, contextExecutor);

		return done;

	}

	@Override
	public void close() {
		client.close();
	}

}

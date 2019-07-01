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
package net.bluemind.webmodule.gwtserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Context;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultContext;
import org.vertx.java.core.impl.VertxInternal;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;

import net.bluemind.lib.vertx.BMExecutor;
import net.bluemind.lib.vertx.BMExecutor.BMTaskMonitor;
import net.bluemind.webmodule.server.HandlerFactory;
import net.bluemind.webmodule.server.NeedVertx;

public class GwtRpcHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(GwtRpcHandler.class);

	private Object delegate;

	private SerializationPolicyProvider serializationPolicyProvider;

	private Vertx vertx;

	public static final BMExecutor executor = new BMExecutor("gwt-rpchandler-thread-");

	public GwtRpcHandler(Object delegate) {
		this(delegate, null);
	}

	public GwtRpcHandler(Object delegate, SerializationPolicyProvider serializationPolicyProvider) {
		this.delegate = delegate;
		this.serializationPolicyProvider = serializationPolicyProvider;
	}

	@Override
	public void handle(HttpServerRequest request) {
		request.bodyHandler(bodyHandler(request));
	}

	private Handler<Buffer> bodyHandler(final HttpServerRequest request) {
		return new Handler<Buffer>() {

			@Override
			public void handle(Buffer buf) {

				Context context = vertx.currentContext();
				executor.execute(new BMExecutor.BMTask() {

					@Override
					public void run(BMTaskMonitor monitor) {
						((VertxInternal) vertx).setContext((DefaultContext) context);
						Thread.currentThread().setContextClassLoader(delegate.getClass().getClassLoader());

						try {
							handleRequest(request, buf);
						} catch (SerializationException e) {
							throw new RuntimeException(e);
						} finally {
							((VertxInternal) vertx).setContext(null);
						}
					}

					@Override
					public void cancelled() {
						reply(request, "call timeout", 500);
					}
				});
			}
		};
	}

	protected void handleRequest(HttpServerRequest request, Buffer buf) throws SerializationException {
		try {

			RPCRequest rpcRequest = RPC.decodeRequest(buf.toString("UTF-8"), null, serializationPolicyProvider);
			String rpcResponse = RPC.invokeAndEncodeResponse(delegate, rpcRequest.getMethod(),
					rpcRequest.getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
			reply(request, rpcResponse, 200);
		} catch (IncompatibleRemoteServiceException ex) {
			logger.error("An IncompatibleRemoteServiceException was thrown while processing this call.", ex);
			reply(request, RPC.encodeResponseForFailure(null, ex), 500);
		} catch (RpcTokenException tokenException) {
			logger.error("An RpcTokenException was thrown while processing this call.", tokenException);
			reply(request, RPC.encodeResponseForFailure(null, tokenException), 500);
		} catch (com.google.gwt.user.server.rpc.UnexpectedException e) {
			logger.error("Unexcepted exception during call", e);
			reply(request, "error :" + e.getMessage(), 500);
		} catch (Exception e) {
			logger.error("Unexcepted exception during call", e);
			reply(request, "error :" + e.getMessage(), 500);
		}

	}

	private void reply(HttpServerRequest request, String invokeAndEncodeResponse, int code) {
		vertx.runOnContext(new Handler<Void>() {

			@Override
			public void handle(Void event) {
				request.response().setStatusCode(code);
				request.response().end(invokeAndEncodeResponse);
			}
		});
	}

	public static HandlerFactory<HttpServerRequest> factory(final Object delegate,
			final SerializationPolicyProvider pp) {
		return new HandlerFactory<HttpServerRequest>() {

			@Override
			public Handler<HttpServerRequest> create(Vertx vertx) {
				return new GwtRpcHandler(delegate, pp);
			}
		};
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
}

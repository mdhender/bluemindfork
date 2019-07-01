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
package net.bluemind.dav.server.proto;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.dav.server.routing.ErrorHandler;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.lib.vertx.VertxPlatform;

public final class DavMethod<Q, R> {

	private static final Logger logger = LoggerFactory.getLogger(DavMethod.class);

	private final IDavProtocol<Q, R> protocol;
	private final String busAddress;

	public DavMethod(IDavProtocol<Q, R> protocol) {
		this(protocol, null);
	}

	public DavMethod(IDavProtocol<Q, R> protocol, String busAddress) {
		this.protocol = protocol;
		this.busAddress = busAddress;
	}

	public void davMethod(final LoggedCore lc, DavResource res, final HttpServerRequest r) {
		r.exceptionHandler(new ErrorHandler(lc, r));
		protocol.parse(r, res, new Handler<Q>() {
			public void handle(Q parsed) {
				if (busAddress == null) {
					protocol.execute(lc, parsed, new Handler<R>() {

						@Override
						public void handle(R event) {
							protocol.write(event, r.response());
						}
					});
				} else {
					busCall(lc, r, parsed);
				}
			}

			private void busCall(final LoggedCore lc, final HttpServerRequest r, Q parsed) {
				final long start = System.nanoTime();
				JsonObject asJson = asJson(lc, parsed);
				VertxPlatform.eventBus().send(busAddress, asJson, new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						long end = System.nanoTime();
						long totalMs = TimeUnit.NANOSECONDS.toMillis(end - start);
						logger.info("{} in {}ms.", busAddress, totalMs);
						LocalJsonObject<?> obj = (LocalJsonObject<?>) event.body();
						HttpServerResponse httpResp = r.response();
						if (obj.getValue() != null && Throwable.class.isAssignableFrom(obj.getValue().getClass())) {
							Throwable t = (Throwable) obj.getValue();
							logger.error(t.getMessage(), t);
							httpResp.setStatusCode(500).setStatusMessage("" + t.getMessage()).end();
						} else if (obj.getValue() == null) {
							logger.error("woop woop null response from protocol !");
							httpResp.setStatusCode(500).setStatusMessage("null response").end();
						} else {
							@SuppressWarnings("unchecked")
							R resp = ((LocalJsonObject<R>) obj).getValue();
							protocol.write(resp, httpResp);
						}
					}
				});
			}
		});
	}

	private JsonObject asJson(LoggedCore lc, Q query) {

		LocalJsonObject<MethodMessage<Q>> ljo = new LocalJsonObject<>(new MethodMessage<Q>(lc, query));
		return ljo;
	}

}

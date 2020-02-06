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
package net.bluemind.eas.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.device.DeviceValidationRequest;
import net.bluemind.eas.dto.device.DeviceValidationResponse;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.vertx.common.LocalJsonObject;
import net.bluemind.vertx.common.request.Requests;

public class DeviceValidationHandler implements Handler<AuthenticatedEASQuery> {

	private static final Logger logger = LoggerFactory.getLogger(DeviceValidationHandler.class);
	private Handler<AuthorizedDeviceQuery> next;
	private final EventBus eb;
	private Vertx vertx;

	public DeviceValidationHandler(Vertx vertx, Handler<AuthorizedDeviceQuery> next) {
		this.vertx = vertx;
		this.next = next;
		eb = vertx.eventBus();
	}

	@Override
	public void handle(AuthenticatedEASQuery event) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] validate device", event.loginAtDomain());
		}

		event.request().pause();
		asyncValidate(event);
	}

	private void asyncValidate(final AuthenticatedEASQuery event) {
		DeviceValidationRequest validationRequest = new DeviceValidationRequest();
		validationRequest.loginAtDomain = event.loginAtDomain();
		validationRequest.password = event.sid();
		validationRequest.deviceIdentifier = event.deviceIdentifier();
		validationRequest.deviceType = event.deviceType();

		if (logger.isDebugEnabled()) {
			logger.debug("Sending to validation: {}", event.deviceIdentifier());
		}

		eb.request(EasBusEndpoints.DEVICE_VALIDATION, new LocalJsonObject<>(validationRequest),
				new Handler<AsyncResult<Message<LocalJsonObject<DeviceValidationResponse>>>>() {

					@Override
					public void handle(AsyncResult<Message<LocalJsonObject<DeviceValidationResponse>>> msg) {
						final HttpServerRequest httpReq = event.request();
						httpReq.resume();
						if (msg.failed()) {
							httpReq.endHandler(new Handler<Void>() {

								@Override
								public void handle(Void event) {
									httpReq.response().setStatusCode(500).setStatusMessage(msg.cause().getMessage())
											.end();
								}
							});
							return;
						}

						DeviceValidationResponse validationResponse = msg.result().body().getValue();
						if (validationResponse.success) {
							AuthorizedDeviceQuery authorized = new AuthorizedDeviceQuery(vertx, event,
									validationResponse.internalId);
							Requests.tag(event.request(), "partnership", validationResponse.internalId);
							next.handle(authorized);
						} else {
							logger.warn("[{}] device {} not authorized.", event.loginAtDomain(),
									event.deviceIdentifier());
							httpReq.endHandler(new Handler<Void>() {

								@Override
								public void handle(Void event) {
									httpReq.response().setStatusCode(403).setStatusMessage("Device is not authorized")
											.end();
								}
							});
						}
					}
				});

	}

}

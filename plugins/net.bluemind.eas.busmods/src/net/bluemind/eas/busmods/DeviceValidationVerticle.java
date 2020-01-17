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
package net.bluemind.eas.busmods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.device.DeviceValidationRequest;
import net.bluemind.eas.dto.device.DeviceValidationResponse;
import net.bluemind.eas.partnership.IDevicePartnershipProvider;
import net.bluemind.eas.partnership.Provider;
import net.bluemind.vertx.common.LocalJsonObject;

public class DeviceValidationVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(DeviceValidationVerticle.class);
	private Handler<Message<LocalJsonObject<DeviceValidationRequest>>> validationHandler;

	@Override
	public void start() {

		validationHandler = new Handler<Message<LocalJsonObject<DeviceValidationRequest>>>() {

			@Override
			public void handle(final Message<LocalJsonObject<DeviceValidationRequest>> msg) {
				IDevicePartnershipProvider partProv = Provider.get();
				partProv.setupAndCheck(msg.body().getValue(), new Handler<DeviceValidationResponse>() {

					@Override
					public void handle(DeviceValidationResponse ev) {
						if (logger.isDebugEnabled()) {
							logger.debug("Sending partnership response, success: {}, id: {}", ev.success,
									ev.internalId);
						}
						LocalJsonObject<DeviceValidationResponse> jso = new LocalJsonObject<>(ev);
						msg.reply(jso);
					}
				});

			}
		};
		vertx.eventBus().consumer(EasBusEndpoints.DEVICE_VALIDATION, validationHandler);
	}

}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.security.certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.system.service.certificate.engine.CertifEngineFactory;

public class CertificateUpdateVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(CertificateUpdateVerticle.class);

	public static boolean suspended = false;

	@Override
	public void start() {
		vertx.eventBus().consumer("domainsettings.config.updated", this::domainSettingsEvent);
	}

	private void domainSettingsEvent(Message<JsonObject> event) {
		if (suspended) {
			logger.warn("CertificateUpdateVerticle does not read domains settings");
			return;
		}

		JsonObject jsonBody = event.body();
		Boolean externalUpdated = jsonBody.getBoolean("externalUrlUpdated");
		if (externalUpdated != null && externalUpdated) {
			CertifEngineFactory.get(jsonBody.getString("domainUid"))
					.ifPresent(c -> c.externalUrlUpdated(Strings.isNullOrEmpty(jsonBody.getString("externalUrlNew"))));

		}
	}
}

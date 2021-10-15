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
package net.bluemind.webmodules.login.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.webmodules.login.services.ReadDomainsSettingService;

public class LoginHandlerVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(LoginHandlerVerticle.class);
	private static final String BM_EXTERNAL_URL_FILEPATH = "/etc/bm/domains-settings";

	public static boolean suspended = false;

	@Override
	public void start() {
		domainSettingsEvent(BM_EXTERNAL_URL_FILEPATH);

		MQ.init(() -> MQ.registerConsumer("end.domain.settings.file.updated", msg -> {
			String filepath = msg.toJson().getString("filepath");
			domainSettingsEvent(filepath);
		}));
	}

	private void domainSettingsEvent(String filepath) {

		if (suspended) {
			logger.warn("LoginHandlerVerticle does not read domains settings file");
			return;
		}

		try {
			ReadDomainsSettingService readDomainsSettings = ReadDomainsSettingService.build(filepath);
			readDomainsSettings.sync();
			logger.info("Update HPS with domains-settings file {}", filepath);
		} catch (Exception e) {
			logger.error("Error during read domains settings file", e);
		}

	}
}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.keycloak.service.domainhook;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.keycloak.verticle.KeycloakVerticleAddress;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class SysConfObserver implements ISystemConfigurationObserver {

	private static final Logger logger = LoggerFactory.getLogger(SysConfObserver.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {

		if (!previous.values.containsKey(SysConfKeys.external_url.name())) {
			// external_url was not set yet (fresh install?). Do not trigger keycloak update
			return;
		}

		boolean needUpdate = hasSysConfChanged(SysConfKeys.external_url, previous, conf)
				|| hasSysConfChanged(SysConfKeys.other_urls, previous, conf);

		if (needUpdate) {
			logger.info("SystemConf has changed, update Keycloack configuration");
			VertxPlatform.eventBus().publish(KeycloakVerticleAddress.UPDATED, new JsonObject());
		}
	}

	private boolean hasSysConfChanged(SysConfKeys prop, SystemConf previous, SystemConf conf) {
		return !Optional.ofNullable(previous.values.get(prop.name())).orElse("")
				.equals(Optional.ofNullable(conf.values.get(prop.name())).orElse(""));
	}

}

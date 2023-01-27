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
package net.bluemind.openid.configuration.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.openid.utils.OpenIdServerConfiguration;

public class OpenIdConfigurationVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(OpenIdConfigurationVerticle.class);

	@Override
	public void start() {
		MQ.init().thenAccept(v -> {
			IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IDomains domApi = sysprov.instance(IDomains.class);
			domApi.all().stream().filter(d -> !"global.virt".equals(d.uid)).forEach(dom -> {
				IDomainSettings domSettingsApi = sysprov.instance(IDomainSettings.class, dom.uid);
				OpenIdServerConfiguration.setDomainSettings(dom.uid, domSettingsApi.get());
				logger.info("OpenIdConfigurationVerticle pre-load domain settings for {}", dom.uid);
			});

		}).exceptionally(t -> {
			logger.error(t.getMessage(), t);
			return null;
		});
	}
}

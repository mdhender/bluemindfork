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
package net.bluemind.system.ldap.export.verticle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.system.ldap.export.internal.LdapExportService;

public class LdapExportVerticle extends Verticle {
	private static final Logger logger = LoggerFactory.getLogger(LdapExportVerticle.class);

	public static boolean suspended = false;

	@Override
	public void start() {
		getVertx().eventBus().registerHandler("dir.changed", new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				if (suspended) {
					logger.warn("LDAP export is suspended");
					return;
				}

				String domain = event.body().getString("domain");

				long time = System.currentTimeMillis();

				try {
					LdapExportService ldapExportService = LdapExportService.build(domain);
					if (ldapExportService != null) {
						logger.info("Update LDAP with changes from domain {}", domain);
						ldapExportService.sync();
					}
				} catch (Exception e) {
					logger.error("Error during LDAP update", e);
				}

				logger.info("Update LDAP with changes from domain {} DONE in {} ms", domain,
						(System.currentTimeMillis() - time));
			}
		});
	}
}

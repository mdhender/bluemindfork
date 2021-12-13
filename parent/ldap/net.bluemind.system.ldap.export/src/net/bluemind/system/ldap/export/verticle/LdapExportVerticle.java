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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.system.ldap.export.services.LdapExportService;
import net.bluemind.system.ldap.export.services.PasswordLifetimeService;
import net.bluemind.system.ldap.export.services.PasswordUpdateService;
import net.bluemind.user.api.UsersHookAddress;

public class LdapExportVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(LdapExportVerticle.class);

	public static boolean suspended = false;

	@Override
	public void start() {
		vertx.eventBus().consumer(DirEventProducer.address, this::dirChangedEvent);
		vertx.eventBus().consumer("domainsettings.updated", this::domainSettingsEvent);
		vertx.eventBus().consumer(UsersHookAddress.PASSWORD_UPDATED, this::userPasswordUpdatedEvent);

		if (!suspended) {
			MQManager.init();
		}
	}

	private void dirChangedEvent(Message<JsonObject> event) {
		if (suspended) {
			logger.warn("LDAP export is suspended");
			return;
		}

		String domain = event.body().getString("domain");

		long time = System.currentTimeMillis();

		try {
			Optional<LdapExportService> ldapExportService = LdapExportService.build(domain);
			if (ldapExportService.isPresent()) {
				logger.info("Update LDAP with changes from domain {}", domain);
				ldapExportService.get().sync();
			}
		} catch (Exception e) {
			logger.error("Error during LDAP update", e);
		}

		logger.info("Update LDAP with changes from domain {} DONE in {} ms", domain,
				(System.currentTimeMillis() - time));
	}

	private void domainSettingsEvent(Message<JsonObject> event) {
		if (suspended) {
			logger.warn("LDAP export is suspended");
			return;
		}

		String domain = event.body().getString("containerUid");

		long time = System.currentTimeMillis();

		try {
			Optional<PasswordLifetimeService> passwordLifetimeUpdater = PasswordLifetimeService.build(domain);
			if (passwordLifetimeUpdater.isPresent()) {
				logger.info("Update LDAP with changes from domain {} settings", domain);
				passwordLifetimeUpdater.get().sync();
			}
		} catch (Exception e) {
			logger.error("Error during LDAP update", e);
		}

		logger.info("Update LDAP with changes from domain {} settings DONE in {} ms", domain,
				(System.currentTimeMillis() - time));
	}

	private void userPasswordUpdatedEvent(Message<JsonObject> event) {
		if (suspended) {
			logger.warn("LDAP export is suspended");
			return;
		}

		String domain = event.body().getString("domain");
		String userUid = event.body().getString("uid");

		long time = System.currentTimeMillis();

		try {
			Optional<PasswordUpdateService> passwordLastChangeUpdater = PasswordUpdateService.build(domain, userUid);
			if (passwordLastChangeUpdater.isPresent()) {
				logger.info("Update LDAP user {}, domain {} password last change", userUid, domain);
				passwordLastChangeUpdater.get().sync();
			}
		} catch (Exception e) {
			logger.error("Error during LDAP update", e);
		}

		logger.info("Update LDAP user {}, domain {} password last change", userUid, domain,
				(System.currentTimeMillis() - time));
	}
}

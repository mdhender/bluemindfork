/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.custom.password.sizestrength.upgrader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrenghtSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;

public class MoveConfToGlobalSettings implements Updater {
	private static final Logger logger = LoggerFactory.getLogger(MoveConfToGlobalSettings.class);

	private static final String CONF_FILE = "/etc/bm/password.ini";

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		monitor.begin(2, "Move password sizestrength configuration to global settings");

		Map<String, String> globalSettings = readConfFromFile();
		monitor.progress(1, String.format("Configuration read from file %s", CONF_FILE));

		try {
			ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGlobalSettings.class)
					.set(globalSettings);
			monitor.progress(1, String.format("Global settings sets from file %s", CONF_FILE));
		} catch (Exception e) {
			monitor.progress(1,
					String.format("Fail to set global settings from file %s: %s", CONF_FILE, e.getMessage()));
			logger.error("Fail to set global setting from file {}", CONF_FILE, e);
			return UpdateResult.failed();
		}

		return UpdateResult.ok();
	}

	private Map<String, String> readConfFromFile() {
		Map<String, String> globalSettings = new HashMap<>();
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name(),
				Boolean.TRUE.toString());
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name(), "6");
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name(), "1");
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name(), "1");
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name(), "1");
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name(), "1");

		Properties p = new Properties();
		try (InputStream fis = Files.newInputStream(Paths.get(CONF_FILE))) {
			p.load(fis);
		} catch (IOException e) {
			return globalSettings;
		}

		Integer digit = getProperty(p, "digit", 1);
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name(), digit.toString());

		Integer capital = getProperty(p, "capital", 1);
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name(), capital.toString());

		Integer lower = getProperty(p, "lower", 1);
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name(), lower.toString());

		Integer special = getProperty(p, "special", 1);
		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name(), special.toString());

		Integer length = getProperty(p, "length", 6);

		globalSettings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name(),
				length < digit + capital + lower + special ? new Integer(digit + capital + lower + special).toString()
						: length.toString());

		logger.info("Password sizestrenght policy readed from {}: {}", CONF_FILE,
				String.join(",", globalSettings.entrySet().stream()
						.map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.toList())));

		return globalSettings;
	}

	private Integer getProperty(Properties p, String name, int defaultValue) {
		Integer value = defaultValue;
		try {
			value = new Integer(p.getProperty(name));
		} catch (NumberFormatException nfe) {
		}

		return value;
	}

	@Override
	public Date date() {
		return java.sql.Date.valueOf(LocalDate.of(2020, 5, 27));
	}

	@Override
	public int sequence() {
		return 100;
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}

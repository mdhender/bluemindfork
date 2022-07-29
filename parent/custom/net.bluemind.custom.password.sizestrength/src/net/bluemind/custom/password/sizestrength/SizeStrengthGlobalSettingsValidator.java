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
package net.bluemind.custom.password.sizestrength;

import java.util.Map;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrenghtSettingsKeys;
import net.bluemind.system.api.GlobalSettings;

public class SizeStrengthGlobalSettingsValidator implements IValidator<GlobalSettings> {
	public static class factory implements IValidatorFactory<GlobalSettings> {

		@Override
		public Class<GlobalSettings> support() {
			return GlobalSettings.class;
		}

		@Override
		public IValidator<GlobalSettings> create(BmContext context) {
			return new SizeStrengthGlobalSettingsValidator();
		}
	}

	@Override
	public void create(GlobalSettings newValue) {
		check(newValue);
	}

	@Override
	public void update(GlobalSettings oldValue, GlobalSettings newValue) {
		check(newValue);
	}

	private void check(GlobalSettings newValue) {
		if (Boolean.valueOf(
				newValue.settings.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name()))) {
			return;
		}

		Integer capital = checkGlobalSettingsValue(newValue.settings,
				PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name());
		Integer digit = checkGlobalSettingsValue(newValue.settings,
				PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name());
		Integer lower = checkGlobalSettingsValue(newValue.settings,
				PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name());
		Integer punct = checkGlobalSettingsValue(newValue.settings,
				PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name());

		Integer minimumLength = checkGlobalSettingsValue(newValue.settings,
				PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name());

		if (minimumLength < capital + digit + lower + punct) {
			throw new ServerFault(
					String.format("Minimum length must be greater or equal than %s", capital + digit + lower + punct),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private static Integer checkGlobalSettingsValue(Map<String, String> globalSettings, String key) {
		try {
			return Integer.valueOf(globalSettings.get(key));
		} catch (NumberFormatException nfe) {
			throw new ServerFault(String.format("Invalid %s, must be an integer", key), ErrorCode.INVALID_PARAMETER);
		}
	}
}

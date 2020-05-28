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

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrenghtSettingsKeys;
import net.bluemind.system.api.GlobalSettings;

public class SizeStrengthGlobalSettingsSanitizer implements ISanitizer<GlobalSettings> {
	public static class factory implements ISanitizerFactory<GlobalSettings> {

		@Override
		public Class<GlobalSettings> support() {
			return GlobalSettings.class;
		}

		@Override
		public ISanitizer<GlobalSettings> create(BmContext context) {
			return new SizeStrengthGlobalSettingsSanitizer();
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
		newValue.settings.put(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name(),
				Boolean.valueOf(
						newValue.settings.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name()))
						.toString());
	}
}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.custom.password.sizestrength;

import java.util.Map;
import java.util.Optional;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrenghtSettingsKeys;
import net.bluemind.custom.password.sizestrength.api.PasswordSizeStrengthDefaultValues;
import net.bluemind.system.api.IGlobalSettings;

public class StrengthPolicy {
	public final boolean enabled;
	public final int minimumLength;
	public final int minimumDigit;
	public final int minimumCapital;
	public final int minimumLower;
	public final int minimumPunct;

	public static StrengthPolicy build() {
		Map<String, String> globalSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class).get();

		if (!Boolean
				.valueOf(globalSettings.get(PasswordSizeStrenghtSettingsKeys.password_sizestrength_enabled.name()))) {
			return new StrengthPolicy(false);
		}

		return new StrengthPolicy(true,
				getGlobalSettingsValue(globalSettings,
						PasswordSizeStrenghtSettingsKeys.password_sizestrength_minimumlength.name()),
				getGlobalSettingsValue(globalSettings,
						PasswordSizeStrenghtSettingsKeys.password_sizestrength_punct.name()),
				getGlobalSettingsValue(globalSettings,
						PasswordSizeStrenghtSettingsKeys.password_sizestrength_lower.name()),
				getGlobalSettingsValue(globalSettings,
						PasswordSizeStrenghtSettingsKeys.password_sizestrength_capital.name()),
				getGlobalSettingsValue(globalSettings,
						PasswordSizeStrenghtSettingsKeys.password_sizestrength_digit.name()));
	}

	private static Optional<Integer> getGlobalSettingsValue(Map<String, String> globalSettings, String key) {
		try {
			return Optional.ofNullable(new Integer(globalSettings.get(key)));
		} catch (NumberFormatException nfe) {
			return Optional.empty();
		}
	}

	private StrengthPolicy(boolean enabled, Optional<Integer> minimumLength, Optional<Integer> minimumPunct,
			Optional<Integer> minimumLower, Optional<Integer> minimumCapital, Optional<Integer> minimumDigit) {
		this.enabled = enabled;

		this.minimumPunct = minimumPunct.orElse(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_PUNCT);
		this.minimumLower = minimumLower.orElse(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LOWER);
		this.minimumCapital = minimumCapital.orElse(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_CAPITAL);
		this.minimumDigit = minimumDigit.orElse(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_DIGIT);

		this.minimumLength = checkMinimumLength(
				minimumLength.orElse(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LENGTH));
	}

	private int checkMinimumLength(Integer minimumLength) {
		return minimumLength < this.minimumDigit + this.minimumCapital + this.minimumLower + this.minimumPunct
				? this.minimumDigit + this.minimumCapital + this.minimumLower + this.minimumPunct
				: minimumLength;
	}

	private StrengthPolicy(boolean enabled) {
		this.enabled = enabled;

		this.minimumPunct = PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_PUNCT;
		this.minimumLower = PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LOWER;
		this.minimumCapital = PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_CAPITAL;
		this.minimumDigit = PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_DIGIT;

		this.minimumLength = checkMinimumLength(PasswordSizeStrengthDefaultValues.DEFAULT_MINIMUM_LENGTH);
	}
}

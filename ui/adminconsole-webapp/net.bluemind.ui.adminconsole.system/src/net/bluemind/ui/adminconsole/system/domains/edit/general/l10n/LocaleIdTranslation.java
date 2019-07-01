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
package net.bluemind.ui.adminconsole.system.domains.edit.general.l10n;

public enum LocaleIdTranslation {
	de("Deutsch"), en("English"), es("Español"), fr("Français"), it("Italiano"), pl("Polski"), sk("Slovenský"), zh(
			"中国的"), hu("Magyar");

	private final String language;
	public static final String DEFAULT_ID = "fr";

	private LocaleIdTranslation(String language) {
		this.language = language;
	}

	public String getLanguage() {
		return language;
	}

	public static String getLanguageById(String id) {
		if (null == id || id.trim().length() == 0) {
			return getLanguageById(DEFAULT_ID);
		}
		return LocaleIdTranslation.valueOf(id.toLowerCase()).getLanguage();
	}

	public static String getIdByLanguage(String language) {
		for (LocaleIdTranslation translation : LocaleIdTranslation.values()) {
			if (translation.language.equals(language)) {
				return translation.name();
			}
		}
		return DEFAULT_ID;
	}
}

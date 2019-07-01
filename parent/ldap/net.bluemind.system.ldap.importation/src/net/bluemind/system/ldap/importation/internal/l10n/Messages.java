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
package net.bluemind.system.ldap.importation.internal.l10n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private ResourceBundle bundle;

	public static Messages get(Locale locale) {
		Messages messages = new Messages();

		if (locale == null) {
			locale = new Locale("en");
		}

		try {
			messages.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/messages", locale);
		} catch (MissingResourceException mre) {
			messages.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/messages");
		}

		return messages;
	}

	public String invalidLoginDn() {
		return bundle.getString("invalidLoginDn");
	}

	public String invalidBaseDn() {
		return bundle.getString("invalidBaseDn");
	}

	public String nullLdapProtocol() {
		return bundle.getString("nullLdapProtocol");
	}

	public String invalidProtocol() {
		return bundle.getString("invalidProtocol");
	}

	public String invalidHostname() {
		return bundle.getString("invalidHostname");
	}

	public String invalidUserFilter() {
		return bundle.getString("invalidUserFilter");
	}

	public String invalidGroupFilter() {
		return bundle.getString("invalidGroupFilter");
	}
}

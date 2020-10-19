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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@SuppressWarnings("serial")
public class EmailAddress extends InternetAddress {

	private static Pattern emailAddress = Pattern.compile("(.+)(<(.*?)>)");

	public EmailAddress(String address) throws AddressException {
		super(sanitizeAddress(address));
	}

	private static String sanitizeAddress(String address) {
		if (address.startsWith("'") && address.endsWith("'")) {
			address = address.substring(1, address.length() - 1);
		}

		Matcher m = emailAddress.matcher(address);
		if (m.matches()) {
			String dn = m.group(1);
			if (!dn.startsWith("\"")) {
				address = address.replace(dn.trim(), "\"" + dn.trim() + "\"");
			}
		}

		return address;
	}
}

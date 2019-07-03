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
package net.bluemind.imap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple mail address representation
 * 
 * 
 */
public final class Address {

	private String mail;
	private String displayName;

	private static final Logger logger = LoggerFactory.getLogger(Address.class);

	public Address(String mail) throws AddressBuildingException {
		this(null, mail);
	}

	public Address(String displayName, String mail) throws AddressBuildingException {
		if (displayName != null) {
			this.displayName = sanitize(displayName);
		}
		if (mail != null && mail.contains("@")) {
			this.mail = sanitize(mail);
		} else {
			logger.error("mail '" + mail + "' is invalid.");
			throw new AddressBuildingException("mail '" + mail + "' is invalid.");
		}
	}

	public String getMail() {
		return mail;
	}

	public String getDisplayName() {
		return displayName != null ? displayName : mail;
	}

	@Override
	public boolean equals(Object obj) {
		return mail.equals(((Address) obj).mail);
	}

	@Override
	public String toString() {
		return "" + displayName + " <" + mail + ">";
	}

	@Override
	public int hashCode() {
		return mail.hashCode();
	}

	private String sanitize(String s) {
		StringBuilder sb = new StringBuilder(s.length());

		char[] chars = s.toCharArray();
		for (char c : chars) {
			if (c != '"' && c != '<' && c != '>') {
				sb.append(c);
			}
		}

		return sb.toString();
	}

}

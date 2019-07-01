/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 20122015
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
package net.bluemind.core.email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class EmailHelper {
	private static final String EMAIL = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$";
	public static final Pattern emailPattern = Pattern.compile(EMAIL);

	public static Collection<Email> sanitizeAndValidate(Collection<Email> emails) throws ServerFault {
		if (emails == null) {
			return null;
		}

		ArrayList<Email> sanitized = new ArrayList<Email>(emails.size());

		for (Email email : emails) {
			String address = email.address.trim().toLowerCase();

			if (!emailPattern.matcher(address).matches()) {
				throw new ServerFault("Invalid email address: " + address, ErrorCode.INVALID_PARAMETER);
			}

			email.address = address;
			sanitized.add(email);
		}

		return sanitized;
	}

	public static Collection<Email> sanitize(Collection<Email> emails) {
		if (emails == null) {
			return null;
		}

		ArrayList<Email> sanitized = new ArrayList<Email>(emails.size());

		for (Email email : emails) {
			String address = email.address.trim().toLowerCase();
			email.address = address;
			sanitized.add(email);
		}

		return sanitized;
	}

	public static void validate(Collection<Email> emails) throws ServerFault {
		if (emails == null) {
			return;
		}

		for (Email email : emails) {
			if (!emailPattern.matcher(email.address).matches()) {
				throw new ServerFault("Invalid email address: '" + email.address + "'", ErrorCode.INVALID_PARAMETER);
			}
		}

	}

	public static void validate(String address) throws ServerFault {
		if (!emailPattern.matcher(address).matches()) {
			throw new ServerFault("Invalid email address: " + address, ErrorCode.INVALID_PARAMETER);
		}
	}

	public static boolean isValid(String address) {
		return emailPattern.matcher(address).matches();
	}

}

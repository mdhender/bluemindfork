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
package net.bluemind.core.sendmail;

import org.apache.james.mime4j.codec.EncoderUtil;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendmailHelper {

	private static final Logger logger = LoggerFactory.getLogger(SendmailHelper.class);

	public static Mailbox formatAddress(String displayName, String em) {
		String email = em != null ? em : "no-reply@blue-mind.net";
		try {
			return AddressBuilder.DEFAULT
					.parseMailbox((displayName != null ? EncoderUtil.encodeAddressDisplayName(displayName) : "") + " <"
							+ email + ">");
		} catch (ParseException e) {
			logger.error("addr parse exception for dn: " + displayName + " e: " + email + " (" + e.getMessage() + ")");
			return null;
		}
	}
}

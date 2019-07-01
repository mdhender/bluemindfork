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
package net.bluemind.imap.sieve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SieveResponseParser {

	private static final Logger logger = LoggerFactory.getLogger(SieveResponseParser.class);

	public SieveResponse parse(SieveMessage sm) {
		StringBuffer b = new StringBuffer();
		for (String l : sm.getLines()) {
			b.append(l);
			b.append("\r\n");
		}

		SieveResponse resp = new SieveResponse(sm.getResponseMessage(), sm.getLines());
		if (logger.isDebugEnabled()) {
			logger.debug("response: '" + resp.getMessageResponse());
		}
		return resp;
	}

}

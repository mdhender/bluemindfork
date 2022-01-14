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
package net.bluemind.lmtp.filter.imip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;

public abstract class CancelHandler extends AbstractLmtpHandler {

	public CancelHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(CancelHandler.class);

	protected boolean validate(IMIPInfos imip) {

		if (imip.iCalendarElements.isEmpty()) {
			logger.info("[{}] Event does not exist in BM, nothing to do.", imip.messageId);
			return false;
		}

		return true;

	}

}

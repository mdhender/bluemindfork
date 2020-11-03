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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;

public abstract class ReplyHandler extends AbstractLmtpHandler {

	public ReplyHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);

	protected boolean validate(IMIPInfos imip, List<Attendee> atts) {

		if (imip.iCalendarElements.isEmpty()) {
			logger.warn("[" + imip.messageId + "] can't handle reply, no VEvents/VTodos found");
			return false;
		}

		if (atts.size() != 1) {
			logger.error("[{}] More than one attendee in reply email, this is invalid ({} attendees)", imip.messageId,
					atts.size());
			return false;
		}

		return true;

	}

}

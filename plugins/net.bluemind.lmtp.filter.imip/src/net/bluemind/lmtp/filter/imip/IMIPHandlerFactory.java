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
import net.bluemind.imip.parser.IMIPInfos.IMIPType;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.lmtp.backend.LmtpAddress;

public abstract class IMIPHandlerFactory {

	private static final Logger logger = LoggerFactory.getLogger(IMIPHandlerFactory.class);

	public static IIMIPHandler get(IMIPInfos imip, LmtpAddress recip, LmtpAddress sender) {
		IMIPType type = imip.type();
		if (null != type) {
			switch (type) {
			case VEVENT:
				return EventIMIPHandlerFactory.get(imip.method, recip, sender);
			case VTODO:
				return TodoIMIPHandlerFactory.get(imip.method, recip, sender);
			}
		}
		logger.warn("No handler for imip info found");
		return null;
	}

	private static abstract class EventIMIPHandlerFactory {

		public static IIMIPHandler get(ITIPMethod method, LmtpAddress recip, LmtpAddress sender) {
			switch (method) {
			case REQUEST:
				return new EventRequestHandler(recip, sender);
			case REPLY:
				return new EventReplyHandler(recip, sender);
			case CANCEL:
				return new EventCancelHandler(recip, sender);
			case DECLINECOUNTER:
				return new EventDeclineCounterHandler();
			case COUNTER:
				return new EventCounterHandler();
			case ADD:
			case PUBLISH:
			case REFRESH:
			default:
				logger.warn("Unsupported IMIP method {}", method);
				return null;
			}
		}
	}

	private static abstract class TodoIMIPHandlerFactory {

		public static IIMIPHandler get(ITIPMethod method, LmtpAddress recip, LmtpAddress sender) {
			switch (method) {
			case REQUEST:
				return new TodoRequestHandler(recip, sender);
			case REPLY:
				return new TodoReplyHandler(recip, sender);
			case CANCEL:
				return new TodoCancelHandler(recip, sender);
			case ADD:
			case COUNTER:
			case DECLINECOUNTER:
			case PUBLISH:
			case REFRESH:
			default:
				logger.warn("Unsupported IMIP method {}", method);
				return null;
			}
		}
	}

}

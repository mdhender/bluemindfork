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

package net.bluemind.dav.server.ics;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.dav.server.store.LoggedCore;

public class FreeBusy {

	private static final Logger logger = LoggerFactory.getLogger(FreeBusy.class);

	public static class CalRequest {
		/**
		 * @param calUid
		 * @param range
		 */
		public CalRequest(String calUid, VFreebusyQuery range) {
			super();
			this.calUid = calUid;
			this.range = range;
		}

		public String calUid;
		public VFreebusyQuery range;

	}

	public static List<CalRequest> parseRequests(byte[] ics, LoggedCore lc) {
		logger.info("[{}] Parse cal requests:\n{}", lc.getUser().value.login, new String(ics));
		List<CalRequest> ret = new LinkedList<FreeBusy.CalRequest>();

		return ret;
	}

}

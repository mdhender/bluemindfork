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
package net.bluemind.calendar.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.hook.internal.VEventMessage;

/**
 * DND Hook
 *
 */
public class XivoHook implements ICalendarHook {

	private static final Logger logger = LoggerFactory.getLogger(XivoHook.class);

	@Override
	public void onEventCreated(VEventMessage message) {
		logger.debug("======================== ON EVENT CREATED {}", message.itemUid);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		logger.debug("======================== ON EVENT UPDATED {}", message.itemUid);
	}

	@Override
	public void onEventDeleted(VEventMessage message) {
		logger.debug("======================== ON EVENT DELETED {}", message.itemUid);
	}

}

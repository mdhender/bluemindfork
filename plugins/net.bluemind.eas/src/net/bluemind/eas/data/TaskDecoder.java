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
package net.bluemind.eas.data;

import org.w3c.dom.Element;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.MSTask;
import net.bluemind.eas.data.email.Type;
import net.bluemind.eas.dto.calendar.CalendarResponse.Sensitivity;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.RTFUtils;

public class TaskDecoder extends Decoder implements IDataDecoder {

	@Override
	public IApplicationData decode(BackendSession bs, Element syncData) {
		MSTask task = new MSTask();

		task.subject = parseDOMString(DOMUtils.getUniqueElement(syncData, "Subject"));

		Element body = DOMUtils.getUniqueElement(syncData, "Body");
		if (body != null) {
			Element data = DOMUtils.getUniqueElement(body, "Data");
			if (data != null) {
				Type bodyType = Type
						.fromInt(Integer.parseInt(DOMUtils.getUniqueElement(body, "Type").getTextContent()));
				String txt = data.getTextContent();
				if (bodyType == Type.PLAIN_TEXT) {
					task.description = data.getTextContent();
				} else if (bodyType == Type.RTF) {
					task.description = RTFUtils.extractB64CompressedRTF(txt);
				} else {
					logger.warn("Unsupported body type: " + bodyType + "\n" + txt);
				}
			}
		}
		Element rtf = DOMUtils.getUniqueElement(syncData, "Compressed_RTF");
		if (rtf != null) {
			String txt = rtf.getTextContent();
			task.description = RTFUtils.extractB64CompressedRTF(txt);
		}

		Integer importance = parseDOMInt(DOMUtils.getUniqueElement(syncData, "Importance"));
		if (importance == null) {
			task.importance = 5;
		} else {
			switch (importance) {
			case 0:
				task.importance = 9;
				break;
			case 1:
				task.importance = 5;
				break;
			case 2:
				task.importance = 1;
				break;
			default:
				task.importance = 5;
			}
		}

		task.utcStartDate = parseDOMDate(DOMUtils.getUniqueElement(syncData, "UtcStartDate"));
		task.startDate = parseDOMDate(DOMUtils.getUniqueElement(syncData, "StartDate"));
		task.utcDueDate = parseDOMDate(DOMUtils.getUniqueElement(syncData, "UtcDueDate"));
		task.dueDate = parseDOMDate(DOMUtils.getUniqueElement(syncData, "DueDate"));
		task.categories = parseDOMStringCollection(DOMUtils.getUniqueElement(syncData, "Categories"), "Category");
		task.complete = parseDOMInt2Boolean(DOMUtils.getUniqueElement(syncData, "Complete"));
		task.dateCompleted = parseDOMDate(DOMUtils.getUniqueElement(syncData, "DateCompleted"));
		task.sensitivity = getCalendarSensitivity(syncData);
		task.reminderTime = parseDOMDate(DOMUtils.getUniqueElement(syncData, "ReminderTime"));
		task.reminderSet = parseDOMInt2Boolean(DOMUtils.getUniqueElement(syncData, "ReminderSet"));

		return task;
	}

	private Sensitivity getCalendarSensitivity(Element domSource) {
		switch (parseDOMNoNullInt(DOMUtils.getUniqueElement(domSource, "Sensitivity"))) {
		case 0:
			return Sensitivity.Normal;
		case 1:
			return Sensitivity.Personal;
		case 2:
			return Sensitivity.Private;
		case 3:
			return Sensitivity.Confidential;
		}
		return null;
	}
}

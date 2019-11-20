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
package net.bluemind.todolist.service.internal;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.todolist.api.VTodo;

/**
 * @author mehdi
 *
 */
public class VTodoValidator {

	/**
	 * @param todo
	 * @throws ServerFault
	 */
	public void validate(VTodo vtodo) throws ServerFault {

		if (vtodo == null) {
			throw new ServerFault("VTodo is null", ErrorCode.INVALID_PARAMETER);
		}

		// RRule
		if (vtodo.rrule != null) {
			VTodo.RRule rrule = vtodo.rrule;
			if (rrule.frequency == null) {
				throw new ServerFault("VTodo.RRule.frequency is null for vtodo.", ErrorCode.INVALID_PARAMETER);
			}
			// rrule until is prior to vevent date BJR(53)
			BmDateTime end = vtodo.due == null ? vtodo.dtstart : vtodo.due;
			if (rrule.until != null && new BmDateTimeWrapper(rrule.until).isBefore(end)) {
				throw new ServerFault("RRule.until is prior to event date", ErrorCode.INVALID_PARAMETER);
			}
		}
		
		validateAttachments(vtodo.attachments);

	}

	private void validateAttachments(List<AttachedFile> attachments) {
		if (attachments != null && !attachments.isEmpty()) {
			for (AttachedFile attachment : attachments) {
				if (StringUtils.isEmpty(attachment.name) || StringUtils.isEmpty(attachment.publicUrl)) {
					throw new ServerFault("Event attachment value is empty", ErrorCode.EMPTY_EVENT_ATTACHMENT_VALUE);
				}
			}
		}

	}
	
}

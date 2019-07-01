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

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

/**
 * Handles external replies : user (organizer) is in bm domain. Process the
 * REPLY email from the external contact.
 * 
 * @author tom
 * 
 */
public class TodoReplyHandler extends ReplyHandler implements IIMIPHandler {

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		List<VEvent.Attendee> atts = imip.iCalendarElements.get(0).attendees;

		if (!super.validate(imip, atts)) {
			return new IMIPResponse();
		}

		try {
			VEvent.Attendee attendee = atts.get(0);

			ITodoList todoService = getTodoListService(getUserFromUid(recipient.getDomainPart(), recipientMailbox.uid));
			ItemValue<VTodo> todo = todoService.getComplete(imip.uid);
			for (VTodo.Attendee a : todo.value.attendees) {
				if (a.mailto.equals(attendee.mailto)) {
					a.partStatus = attendee.partStatus;
				}
			}
			todoService.update(todo.uid, todo.value);

		} catch (Exception e) {
			throw e;
		}
		return new IMIPResponse();
	}
}

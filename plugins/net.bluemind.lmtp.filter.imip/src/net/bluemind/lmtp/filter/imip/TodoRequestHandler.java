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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.user.api.User;

public class TodoRequestHandler extends AbstractLmtpHandler implements IIMIPHandler {

	public TodoRequestHandler(LmtpAddress recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(TodoRequestHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		try {

			if (recipientMailbox.value.type == Mailbox.Type.resource) {
				throw new ServerFault("Unsuported VTodo for recipient: " + recipient.getEmailAddress() + ", kind: "
						+ recipientMailbox.value.type.toString());
			}

			ItemValue<User> user = getUserFromUid(recipient.getDomainPart(), recipientMailbox.uid);

			ITodoList service = getTodoListService(user);

			ItemValue<VTodo> vtodo = service.getComplete(imip.uid);
			if (vtodo != null) {
				long bmSeq = vtodo.version;
				int imipSeq = imip.sequence;
				logger.info("[{}] VTodo already in BM (id: {}) with SEQ: {}, IMIP SEQ: {}", imip.messageId, imip.uid,
						bmSeq, imipSeq);

				if (imipSeq >= bmSeq) {
					update(user, vtodo.uid, imip, imip.iCalendarElements.get(0));
				} else {
					logger.warn("[{}] IMIP seq ({}) is lower or equal to bm seq ({}), doing nothing.", imip.messageId,
							imipSeq, bmSeq);
				}

			} else {
				create(service, user, imip, imip.iCalendarElements.get(0));
			}

			return IMIPResponse.createNeedResponse(imip.uid, imip.iCalendarElements.get(0));

		} catch (Exception e) {
			throw e;
		}
	}

	private void update(ItemValue<User> user, String uid, IMIPInfos imip, ICalendarElement element) throws ServerFault {
		VTodo imipVTodo = (VTodo) element;
		getTodoListService(user).update(uid, imipVTodo);
		logger.info("[{}] VTodo {} updated", imip.messageId, uid);
	}

	/**
	 * @param authKey
	 * @param imip
	 * @param user
	 * @throws ServerFault
	 */
	private void create(ITodoList todoService, ItemValue<User> user, IMIPInfos imip, ICalendarElement element)
			throws ServerFault {
		if (imip.organizerEmail == null) {
			logger.warn("[" + imip.messageId + "] We need a contact to identify the organizer");
		}

		VTodo todo = (VTodo) element;

		todoService.create(imip.uid, todo);
		logger.info("[{}] VTodo {} created", imip.messageId, todo.summary);

	}

}

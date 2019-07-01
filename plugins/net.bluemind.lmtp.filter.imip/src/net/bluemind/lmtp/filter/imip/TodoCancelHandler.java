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
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

/**
 * Handles cancellations of meetings
 * 
 * @author tom
 * 
 */
public class TodoCancelHandler extends CancelHandler implements IIMIPHandler {

	private static final Logger logger = LoggerFactory.getLogger(TodoCancelHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, LmtpAddress recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		if (!super.validate(imip)) {
			return new IMIPResponse();
		}

		try {
			logger.info("[{}] Deleting BM VTodo with id {}", imip.messageId, imip.uid);
			ITodoList todoService = getTodoListService(getUserFromUid(recipient.getDomainPart(), recipientMailbox.uid));
			ItemValue<VTodo> res = todoService.getComplete(imip.uid);

			if (res == null) {
				logger.debug("[{}] BM VTodo with id {}, doesnt exists", imip.messageId, imip.uid);
			} else {
				todoService.delete(imip.uid);
			}
			return new IMIPResponse();
		} catch (Exception e) {
			throw e;
		}
	}

}

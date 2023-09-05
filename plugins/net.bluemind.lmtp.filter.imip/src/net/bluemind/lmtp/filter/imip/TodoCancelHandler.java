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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.delivery.lmtp.common.LmtpAddress;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.domain.api.Domain;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

/**
 * Handles cancellations of todos
 * 
 * @author tom
 * 
 */
public class TodoCancelHandler extends CancelHandler implements IIMIPHandler {

	public TodoCancelHandler(ResolvedBox recipient, LmtpAddress sender) {
		super(recipient, sender);
	}

	private static final Logger logger = LoggerFactory.getLogger(TodoCancelHandler.class);

	@Override
	public IMIPResponse handle(IMIPInfos imip, ResolvedBox recipient, ItemValue<Domain> domain,
			ItemValue<Mailbox> recipientMailbox) throws ServerFault {

		if (!super.validate(imip)) {
			return IMIPResponse.createEmptyResponse();
		}

		try {
			logger.info("[{}] Deleting BM VTodo with ics uid {}", imip.messageId, imip.uid);
			ITodoList todoService = getTodoListService(getUserFromUid(recipient.getDomainPart(), recipientMailbox.uid));
			List<ItemValue<VTodo>> res = todoService.getByIcsUid(imip.uid);

			if (res == null || res.isEmpty()) {
				logger.debug("[{}] BM VTodo with ics uid {}, doesnt exists", imip.messageId, imip.uid);
			} else {
				res.forEach(todo -> todoService.delete(todo.uid));
			}
			return IMIPResponse.createEmptyResponse();
		} catch (Exception e) {
			throw e;
		}
	}

}

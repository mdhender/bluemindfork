/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.IMailboxRecordExpunged;
import net.bluemind.backend.mail.replica.api.MailboxRecordExpunged;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ItemFlagFilter;

public class NoopMailboxRecordExpungedService implements IMailboxRecordExpunged {

	private static final Logger logger = LoggerFactory.getLogger(NoopMailboxRecordExpungedService.class);

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		logger.info("NOOP operation IMailboxRecordExpunged#count");
		return null;
	}

	@Override
	public void delete(long itemId) {
		logger.info("NOOP operation IMailboxRecordExpunged#delete");

	}

	@Override
	public List<MailboxRecordExpunged> fetch() {
		logger.info("NOOP operation IMailboxRecordExpunged#fetch");
		return null;
	}

	@Override
	public MailboxRecordExpunged get(long itemId) {
		logger.info("NOOP operation IMailboxRecordExpunged#get");
		return null;
	}

}

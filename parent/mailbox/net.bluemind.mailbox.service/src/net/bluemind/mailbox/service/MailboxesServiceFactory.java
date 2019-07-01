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
package net.bluemind.mailbox.service;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.AbstractDirServiceFactory;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.service.internal.MailboxesService;

public class MailboxesServiceFactory extends AbstractDirServiceFactory<IMailboxes>
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMailboxes> {

	public MailboxesServiceFactory() {

	}

	@Override
	public Class<IMailboxes> factoryClass() {
		return IMailboxes.class;
	}

	@Override
	protected IMailboxes instanceImpl(BmContext context, ItemValue<Domain> domainValue, Container container)
			throws ServerFault {
		return new MailboxesService(context, container, domainValue);
	}
}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service;

import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.replica.service.internal.OutboxService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class OutboxServiceFactory implements IServerSideServiceFactory<IOutbox>  {

	public OutboxServiceFactory() {
	}

	@Override
	public Class<IOutbox> factoryClass() {
		return IOutbox.class;
	}

	@Override
	public IOutbox instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domainUid = params[0];
		String mailboxUid = params[1];
		
		ItemValue<Mailbox> mailboxItem = ServerSideServiceProvider
			.getProvider(SecurityContext.SYSTEM)
			.instance(IMailboxes.class, domainUid)
			.getComplete(mailboxUid);
		
		return new OutboxService(
				context, 
				domainUid, 
				mailboxItem,
				new RunnableExtensionLoader<ISendmail>()
					.loadExtensionsWithPriority("net.bluemind.core.sendmail", "mailer", "mailer", "impl")
					.get(0)
				);
	}

}

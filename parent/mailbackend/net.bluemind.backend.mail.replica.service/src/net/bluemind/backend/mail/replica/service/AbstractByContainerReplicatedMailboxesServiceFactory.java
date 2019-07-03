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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public abstract class AbstractByContainerReplicatedMailboxesServiceFactory<T>
		extends AbstractReplicatedMailboxesServiceFactory<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMailboxRecordServiceFactory.class);

	protected AbstractByContainerReplicatedMailboxesServiceFactory() {
	}

	private static final Pattern userSubtree = Pattern.compile("subtree_([^!]*)!user\\.(.*)");
	private static final Pattern sharedSubtree = Pattern.compile("subtree_([^!]*)!(.*)");

	@Override
	public T instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 1) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String subtreeContainer = params[0];
		if (logger.isDebugEnabled()) {
			logger.debug("params[0]: " + params[0]);
		}

		Matcher userMatcher = userSubtree.matcher(subtreeContainer);
		Matcher sharedMatcher = sharedSubtree.matcher(subtreeContainer);
		Matcher matched = null;
		Namespace ns = null;
		if (userMatcher.find()) {
			matched = userMatcher;
			ns = Namespace.users;
		} else if (sharedMatcher.find()) {
			matched = sharedMatcher;
			ns = Namespace.shared;
		} else {
			throw new ServerFault("Unknown subtree container format '" + subtreeContainer + "'");
		}
		String partition = matched.group(1);
		String domain = partition.replace('_', '.');
		String ownerUid = matched.group(2);
		IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, domain);
		ItemValue<Mailbox> mailbox = mboxApi.getComplete(ownerUid);

		if (mailbox == null) {
			throw new ServerFault("Mailbox not found '" + ownerUid + "'");
		}

		CyrusPartition cp = CyrusPartition.forServerAndDomain(mailbox.value.dataLocation, domain);
		MailboxReplicaRootDescriptor rootDesc = MailboxReplicaRootDescriptor.create(ns, mailbox.value.name);

		return getService(context, cp, rootDesc);
	}

}

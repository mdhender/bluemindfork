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
package net.bluemind.im.service.internal;

import java.util.List;
import java.util.Set;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.im.api.IInstantMessaging;
import net.bluemind.im.api.IMMessage;
import net.bluemind.im.persistance.IMIndexStore;
import net.bluemind.im.persistance.IMStore;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class IMService implements IInstantMessaging {

	private static final Logger logger = LoggerFactory.getLogger(IInstantMessaging.class);
	private IMStore store;
	private IMIndexStore indexStore;
	private RBACManager rbacManager;
	private BmContext context;

	public IMService(BmContext context, Client es) {
		this.context = context;
		rbacManager = new RBACManager(context);
		store = new IMStore(context.getDataSource());
		indexStore = new IMIndexStore(es);
	}

	@Override
	public void setRoster(String jabberId, String data) throws ServerFault {
		checkAccess(jabberId);
		// sanitizer..
		if (data == null) {
			data = "";
		}
		String oldRoster = store.getRoster(jabberId);
		store.setRoster(jabberId, data);
		logger.debug("{} update roster: old:\n{}\n new:\n{}", jabberId, oldRoster, data);
	}

	@Override
	public String getRoster(String jabberId) throws ServerFault {
		checkAccess(jabberId);
		return store.getRoster(jabberId);
	}

	@Override
	public List<IMMessage> getLastMessagesBetween(String user1, String user2, Integer messagesCount)
			throws ServerFault {
		checkAccess(user1);
		return indexStore.getLastMessagesBetween(user1, user2, messagesCount);
	}

	@Override
	public void sendGroupChatHistory(String sender, String groupChatId, List<String> recipients) throws ServerFault {
		checkAccess(sender);

		List<IMMessage> history = indexStore.getGroupChatHistory(groupChatId);
		SendGroupChatHistory send = new SendGroupChatHistory(sender, history, recipients);
		send.run();
	}

	private void checkAccess(String user1) throws ServerFault {

		if (!rbacManager.can(SecurityContext.ROLE_SYSTEM)) {
			ItemValue<User> user = context.su().provider()
					.instance(IUser.class, context.getSecurityContext().getContainerUid()).byEmail(user1);
			if (user == null) {
				throw new ServerFault(
						"no user " + user1 + " found in domain " + context.getSecurityContext().getContainerUid(),
						ErrorCode.NOT_FOUND);
			}

			if (!user.uid.equals(context.getSecurityContext().getSubject())) {
				throw new ServerFault(
						"not authorized (" + context.getSecurityContext().getSubject() + " cannot do that on " + user1,
						ErrorCode.PERMISSION_DENIED);
			}
		}
	}

	@Override
	public boolean isActiveUser(String uid) throws ServerFault {
		String domain = context.getSecurityContext().getContainerUid();
		Set<String> resolvedRoles = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain).getResolvedRoles(uid);
		for (String value : resolvedRoles) {
			if (value.equals(BasicRoles.ROLE_IM)) {
				return true;
			}
		}
		return false;
	}

}

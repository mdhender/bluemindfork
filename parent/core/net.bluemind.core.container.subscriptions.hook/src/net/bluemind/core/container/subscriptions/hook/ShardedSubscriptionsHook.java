/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.subscriptions.hook;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.hooks.ContainersHookAdapter;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.IUserSubscription;

public class ShardedSubscriptionsHook extends ContainersHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ShardedSubscriptionsHook.class);

	@Override
	public void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs,
			List<String> unsubs) throws ServerFault {
		String domain = cd.domainUid;
		for (String newOwner : subs) {
			IInternalOwnerSubscriptions subApi = ctx.provider().instance(IInternalOwnerSubscriptions.class, domain,
					newOwner);
			ContainerSubscriptionModel sub = ContainerSubscriptionModel.create(cd, cd.offlineSync);
			String subUid = IOwnerSubscriptionUids.subscriptionUid(cd.uid, newOwner);
			if (subApi.get(subUid) == null) {
				subApi.create(subUid, sub);
				logger.info("Stored new sub {}, offline: {}", subUid, cd.offlineSync);
			}
		}
		for (String oldOwner : unsubs) {
			IInternalOwnerSubscriptions subApi = ctx.provider().instance(IInternalOwnerSubscriptions.class, domain,
					oldOwner);
			String subUid = IOwnerSubscriptionUids.subscriptionUid(cd.uid, oldOwner);
			subApi.delete(subUid);
			logger.info("Cleared sub {}", subUid);
		}
	}

	@Override
	public void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject) {
		IInternalOwnerSubscriptions subApi = ctx.provider().instance(IInternalOwnerSubscriptions.class, cd.domainUid,
				subject);
		String sub = IOwnerSubscriptionUids.subscriptionUid(cd.uid, subject);
		ItemValue<ContainerSubscriptionModel> theSub = subApi.getComplete(sub);
		if (theSub != null) {
			theSub.value.offlineSync = cd.offlineSync;
			subApi.update(sub, theSub.value);
			logger.info("Updated sub {} for offlineSync {}", sub, cd.offlineSync);
		}
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		if (cur.name.equals(prev.name) && cur.defaultContainer == prev.defaultContainer) {
			return;
		}
		IUserSubscription userSubApi = ctx.provider().instance(IUserSubscription.class, cur.domainUid);
		// we grab that from t_container_sub in the directory DB
		List<String> subscribers = userSubApi.subscribers(cur.uid);

		for (String subject : subscribers) {
			IInternalOwnerSubscriptions subApi = ctx.provider().instance(IInternalOwnerSubscriptions.class,
					cur.domainUid, subject);
			String sub = IOwnerSubscriptionUids.subscriptionUid(cur.uid, subject);
			ItemValue<ContainerSubscriptionModel> toUpdate = subApi.getComplete(sub);
			logger.info("Renaming {} => {} for subscriber {}", toUpdate.value.name, cur.name, subject);
			toUpdate.value.name = cur.name;
			toUpdate.value.defaultContainer = cur.defaultContainer;
			subApi.update(sub, toUpdate.value);
		}
	}

}

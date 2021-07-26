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
package net.bluemind.directory.service;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptionsMgmt;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public abstract class BaseDirStoreService<T> extends ContainerStoreService<T> {

	private BmContext bmContext;

	protected BaseDirStoreService(BmContext ctx, DataSource pool, SecurityContext securityContext, Container container,
			IItemValueStore<T> itemValueStore) {
		super(pool, securityContext, container, itemValueStore);
		this.bmContext = ctx;
	}

	private void initHierarchy(String uid) {
		IInternalContainersFlatHierarchyMgmt chMgmt = ServerSideServiceProvider.getProvider(bmContext)
				.instance(IInternalContainersFlatHierarchyMgmt.class, container.domainUid, uid);
		chMgmt.init();

		IInternalOwnerSubscriptionsMgmt subMgmt = ServerSideServiceProvider.getProvider(bmContext)
				.instance(IInternalOwnerSubscriptionsMgmt.class, container.domainUid, uid);
		subMgmt.init();
	}

	@Override
	public final ItemVersion create(String uid, String displayName, T value) throws ServerFault {
		return create(uid, null, displayName, value);
	}

	@Override
	public final ItemVersion create(String uid, String extId, String displayName, T value) throws ServerFault {
		ItemVersion ret = super.create(uid, extId, displayName, value);
		initHierarchy(uid);
		return ret;
	}

	@Override
	public final ItemVersion create(Item item, T value) throws ServerFault {
		ItemVersion ret = super.create(item, value);
		initHierarchy(item.uid);
		return ret;
	}

	@Override
	public ItemVersion delete(String uid) throws ServerFault {
		ItemVersion delete = super.delete(uid);
		// FIXME not sure what the right order is as those will sit in separate
		// databases
		if (delete != null) {
			IInternalContainersFlatHierarchyMgmt chMgmt = ServerSideServiceProvider.getProvider(bmContext)
					.instance(IInternalContainersFlatHierarchyMgmt.class, container.domainUid, uid);
			chMgmt.delete();

			IInternalOwnerSubscriptionsMgmt subMgmt = ServerSideServiceProvider.getProvider(bmContext)
					.instance(IInternalOwnerSubscriptionsMgmt.class, container.domainUid, uid);
			subMgmt.delete();
		}
		return delete;
	}

}

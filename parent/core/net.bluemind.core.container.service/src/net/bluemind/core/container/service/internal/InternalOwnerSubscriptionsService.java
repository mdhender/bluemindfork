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
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.OwnerSubscriptionStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.role.api.BasicRoles;

public class InternalOwnerSubscriptionsService implements IInternalOwnerSubscriptions {

	private final BmContext context;
	private final Container container;
	private final ContainerStoreService<ContainerSubscriptionModel> storeService;
	private OwnerSubscriptionsEventProducer eventsProducer;

	public InternalOwnerSubscriptionsService(BmContext context, DataSource ds, Container cont,
			OwnerSubscriptionsEventProducer events) {
		this.context = context;
		this.container = cont;

		// we do that so UserSubscriptionService can list its data from here
		RBACManager rbac = new RBACManager(context).forDomain(context.getSecurityContext().getContainerUid())
				.forEntry(container.owner);
		rbac.check(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, BasicRoles.ROLE_SELF);

		OwnerSubscriptionStore itemValueStore = new OwnerSubscriptionStore(ds, this.container);
		storeService = new ContainerStoreService<>(ds, context.getSecurityContext(), this.container, itemValueStore);
		this.eventsProducer = events;
	}

	@Override
	public void create(String uid, ContainerSubscriptionModel node) throws ServerFault {
		ItemVersion itemVersion = storeService.create(uid, node.containerUid, node);
		eventsProducer.changed(itemVersion.version);
	}

	@Override
	public void createWithId(long id, String uid, ContainerSubscriptionModel node) throws ServerFault {
		ItemVersion itemVersion = storeService.createWithId(uid, id, null, node.containerUid, node);
		eventsProducer.changed(itemVersion.version);
	}

	@Override
	public void update(String uid, ContainerSubscriptionModel node) throws ServerFault {
		ItemVersion itemVersion = storeService.update(uid, node.containerUid, node);
		eventsProducer.changed(itemVersion.version);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		ItemVersion delete = storeService.delete(uid);
		if (delete != null) {
			eventsProducer.changed(delete.version);
		}
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		return storeService.changelog(itemUid, since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemIdentifier> fullChangesetById(Long since) {
		return storeService.fullChangesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		return storeService.changesetById(since == null ? 0L : since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		return storeService.getVersion();
	}

	@Override
	public List<ItemValue<ContainerSubscriptionModel>> list() throws ServerFault {
		return storeService.all();
	}

	@Override
	public ItemValue<ContainerSubscriptionModel> getComplete(String uid) {
		return storeService.get(uid, null);
	}

	@Override
	public void reset() {
		storeService.prepareContainerDelete();
	}

	@Override
	public ItemValue<ContainerSubscriptionModel> getCompleteById(long id) {
		return storeService.get(id, null);
	}

	@Override
	public List<ItemValue<ContainerSubscriptionModel>> getMultipleById(List<Long> id) {
		return storeService.getMultipleById(id);
	}

	@Override
	public List<ItemValue<ContainerSubscriptionModel>> getMultiple(List<String> uids) {
		return storeService.getMultiple(uids);
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		DataSource ds = context.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, context.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		storeService.xfer(ds, c, new OwnerSubscriptionStore(ds, c));
	}

}

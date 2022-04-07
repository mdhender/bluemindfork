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
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.container.service.internal.ContainersHierarchyEventProducer.Operation;
import net.bluemind.core.rest.BmContext;

public class InternalContainersHierarchyService implements IInternalContainersFlatHierarchy, IContainersFlatHierarchy {

	private final BmContext context;
	private final Container container;
	private final ContainerStoreService<ContainerHierarchyNode> storeService;
	private final RBACManager rbacManager;
	private ContainersHierarchyEventProducer eventsProducer;

	public InternalContainersHierarchyService(BmContext context, DataSource ds, Container cont,
			ContainersHierarchyEventProducer events, ContainerStoreService<ContainerHierarchyNode> storeService) {
		this.context = context;
		this.container = cont;
		this.storeService = storeService;
		rbacManager = RBACManager.forContext(this.context).forContainer(container);
		this.eventsProducer = events;
	}

	@Override
	public void create(String uid, ContainerHierarchyNode node) throws ServerFault {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		ItemVersion itemVersion = storeService.create(uid, node.name, node);
		eventsProducer.changed(itemVersion.version, node.containerUid, Operation.CREATE);
	}

	@Override
	public void createWithId(long id, String uid, ContainerHierarchyNode node) throws ServerFault {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		ItemVersion itemVersion = storeService.createWithId(uid, id, null, node.name, node);
		eventsProducer.changed(itemVersion.version, node.containerUid, Operation.CREATE);
	}

	@Override
	public void createItem(Item it, ContainerHierarchyNode node) throws ServerFault {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		ItemVersion itemVersion = storeService.create(it, node);
		eventsProducer.changed(itemVersion.version, node.containerUid, Operation.CREATE);
	}

	@Override
	public void update(String uid, ContainerHierarchyNode node) throws ServerFault {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		ItemVersion itemVersion = storeService.update(uid, node.name, node);
		eventsProducer.changed(itemVersion.version, node.containerUid, Operation.UPDATE);
	}

	@Override
	public void touch(String uid) {
		storeService.touch(uid);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		ItemVersion delete = storeService.delete(uid);
		if (delete != null) {
			eventsProducer.changed(delete.version, ContainerHierarchyNode.extractContainerUid(uid), Operation.DELETE);
		}
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(itemUid, since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changesetById(since == null ? 0L : since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public List<ItemValue<ContainerHierarchyNode>> list() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public ItemValue<ContainerHierarchyNode> getComplete(String uid) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public void reset() {
		rbacManager.check(Verb.Write.name(), Verb.Manage.name());
		storeService.prepareContainerDelete();
	}

	@Override
	public ItemValue<ContainerHierarchyNode> getCompleteById(long id) {
		rbacManager.check(Verb.Read.name());
		return storeService.get(id, null);
	}

	@Override
	public List<ItemValue<ContainerHierarchyNode>> getMultipleById(List<Long> id) {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(id);
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
		storeService.xfer(ds, c, new ContainersHierarchyNodeStore(ds, c));
	}

}

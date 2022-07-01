/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.deferredaction.service.internal;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IInternalDeferredAction;
import net.bluemind.deferredaction.persistence.DeferredActionStore;

public class DeferredActionService implements IInternalDeferredAction {
	private final Container container;
	private final BmContext context;
	private final ContainerDeferredActionStoreService storeService;
	private final DeferredActionStore deferredActionStore;
	private final Validator validator;
	private final Sanitizer sanitizer;

	public DeferredActionService(Container container, DataSource dataSource, BmContext context) {
		this.container = container;
		this.context = context;
		this.deferredActionStore = new DeferredActionStore(dataSource, container);
		this.storeService = new ContainerDeferredActionStoreService(dataSource, context.getSecurityContext(), container,
				deferredActionStore);
		this.validator = new Validator(context);
		this.sanitizer = new Sanitizer(context);
	}

	@Override
	public void create(DeferredAction deferredAction) throws ServerFault {
		byte[] u = (deferredAction.reference + deferredAction.executionDate.toString()).getBytes();
		String uid = UUID.nameUUIDFromBytes(u).toString();
		create(uid, deferredAction);
	}

	@Override
	public void create(String uid, DeferredAction deferredAction) throws ServerFault {
		ItemValue<DeferredAction> deferredActionItem = ItemValue.create(uid, deferredAction);
		deferredActionItem.displayName = displayName(deferredAction);
		create(deferredActionItem);
	}

	private void create(ItemValue<DeferredAction> deferredActionItem) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Write.name());
		DeferredAction deferredAction = deferredActionItem.value;
		sanitizer.create(deferredAction);
		validator.create(deferredAction);

		storeService.create(deferredActionItem.item(), deferredAction);
	}

	@Override
	public void update(String uid, DeferredAction deferredAction) throws ServerFault {
		ItemValue<DeferredAction> deferredActionItem = ItemValue.create(uid, deferredAction);
		deferredActionItem.displayName = displayName(deferredAction);
		update(deferredActionItem);
	}

	private void update(ItemValue<DeferredAction> deferredActionItem) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Write.name());
		String uid = deferredActionItem.uid;
		DeferredAction deferredAction = deferredActionItem.value;
		ItemValue<DeferredAction> current = storeService.get(uid, null);
		sanitizer.update(current, deferredAction);
		validator.update(current, deferredAction);

		storeService.update(deferredActionItem.item(), deferredActionItem.displayName, deferredAction);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Write.name());

		storeService.delete(uid);
	}

	@Override
	public void deleteAll() throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Write.name());

		storeService.deleteAll();
	}

	@Override
	public DeferredAction get(String uid) throws ServerFault {
		ItemValue<DeferredAction> item = getComplete(uid);
		return (item != null) ? item.value : null;
	}

	@Override
	public ItemValue<DeferredAction> getComplete(String uid) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<DeferredAction>> getByActionId(String actionId, Long to) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.getByActionId(actionId, new Date(to));
	}

	@Override
	public List<ItemValue<DeferredAction>> getByReference(String reference) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.getByReference(reference);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

		return storeService.getVersion();
	}

	private String displayName(DeferredAction deferredAction) {
		return deferredAction.actionId + "-" + deferredAction.executionDate.toString();
	}

	@Override
	public List<ItemValue<DeferredAction>> multipleGet(List<String> uids) throws ServerFault {
		RBACManager.forContext(context).forContainer(container).check(Verb.Read.name());

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
		storeService.xfer(ds, c, new DeferredActionStore(ds, c));
	}

	@Override
	public void restore(ItemValue<DeferredAction> deferredActionItem, boolean isCreate) {
		if (isCreate) {
			create(deferredActionItem);
		} else {
			update(deferredActionItem);
		}
	}

}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarViewUids;
import net.bluemind.core.backup.continuous.mgmt.service.containers.addressbook.BookSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.calendar.CalendarSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.calendar.CalendarViewSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.mail.RecordsSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.mail.SubtreeSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.misc.NoteSync;
import net.bluemind.core.backup.continuous.mgmt.service.containers.misc.TodoSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync.Factory;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.todolist.api.ITodoUids;

public class ContainerSyncRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ContainerSyncRegistry.class);

	private ContainerSyncRegistry() {

	}

	private static final Map<String, ContainerSync.Factory> byType = initMap();

	private static Map<String, Factory> initMap() {
		ConcurrentHashMap<String, ContainerSync.Factory> registry = new ConcurrentHashMap<>();

		registry.put(IMailReplicaUids.REPLICATED_MBOXES, new SubtreeSync.SyncFactory());
		registry.put(IMailReplicaUids.MAILBOX_RECORDS, new RecordsSync.SyncFactory());
		registry.put(ICalendarUids.TYPE, new CalendarSync.SyncFactory());
		registry.put(ICalendarViewUids.TYPE, new CalendarViewSync.SyncFactory());
		registry.put(ITodoUids.TYPE, new TodoSync.SyncFactory());
		registry.put(INoteUids.TYPE, new NoteSync.SyncFactory());
		registry.put(IAddressBookUids.TYPE, new BookSync.SyncFactory());

		return registry;
	}

	private static final ContainerSync NOOP = new ContainerSync(null) {

	};

	public static <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
			ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain) {
		Factory factory = byType.get(node.value.containerType);
		if (factory == null) {
			return NOOP;
		}
		IContainers contApi = ctx.provider().instance(IContainers.class);
		if (contApi.getIfPresent(node.value.containerUid) == null) {
			logger.warn("Skip processing of missing container for {}. Owner {} might be archived.", node, owner);
			return NOOP;
		}

		return factory.forNode(ctx, node, owner, domain);

	}

}

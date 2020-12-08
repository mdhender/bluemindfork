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
package net.bluemind.calendar.auditlog;

import java.util.List;

import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.task.api.TaskRef;

public class CalendarAuditProxy implements IInternalCalendar {
	private CalendarAuditor auditor;
	private IInternalCalendar calendar;

	public CalendarAuditProxy(CalendarAuditor auditor, IInternalCalendar cal) {
		this.auditor = auditor;
		this.calendar = cal;
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		return auditor.action("itemChangelog").readOnly().actionItemUid(itemUid).addActionMetadata("since", since)
				.audit(() -> calendar.itemChangelog(itemUid, since));
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		return auditor.action("containerChangelog").readOnly().addActionMetadata("since", since)
				.audit(() -> calendar.containerChangelog(since));
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		return auditor.action("changeset").readOnly().addActionMetadata("since", since)
				.audit(() -> calendar.changeset(since));

	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		return auditor.action("changesetById").readOnly().addActionMetadata("since", since)
				.audit(() -> calendar.changesetById(since));

	}

	@Override
	public long getVersion() throws ServerFault {
		return auditor.action("version").readOnly().audit(() -> calendar.getVersion());

	}

	@Override
	public void create(String uid, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		auditor.actionCreateOn(uid).actionValue(event).withSendNotification(sendNotifications)
				.audit(() -> calendar.create(uid, event, sendNotifications));
	}

	@Override
	public Ack createById(long id, VEventSeries event) throws ServerFault {
		return auditor.actionCreateOn(Long.toString(id)).addActionMetadata("byId", Boolean.TRUE).actionValue(event)
				.withSendNotification(false).audit(() -> calendar.createById(id, event));
	}

	@Override
	public void update(String uid, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		auditor.actionUpdateOn(uid).actionValue(event).withSendNotification(sendNotifications)
				.audit(() -> calendar.update(uid, event, sendNotifications));

	}

	@Override
	public ItemValue<VEventSeries> getComplete(String uid) throws ServerFault {
		return auditor.action("getComplete").readOnly().actionItemUid(uid).audit(() -> calendar.getComplete(uid));
	}

	@Override
	public List<ItemValue<VEventSeries>> getByIcsUid(String uid) throws ServerFault {
		return auditor.action("getByIcsUid").readOnly().addActionMetadata("icsUid", uid)
				.audit(() -> calendar.getByIcsUid(uid));
	}

	@Override
	public List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault {
		return auditor.action("multipleGet").readOnly().addActionMetadata("itemUids", uids)
				.audit(() -> calendar.multipleGet(uids));
	}

	@Override
	public void delete(String uid, Boolean sendNotifications) throws ServerFault {
		auditor.actionDeleteOn(uid).withSendNotification(sendNotifications)
				.audit(() -> calendar.delete(uid, sendNotifications));
	}

	@Override
	public void touch(String uid) throws ServerFault {
		auditor.action("touch").actionItemUid(uid).audit(() -> calendar.touch(uid));
	}

	@Override
	public ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault {
		return calendar.updates(changes);
	}

	@Override
	public ListResult<ItemValue<VEventSeries>> search(VEventQuery query) throws ServerFault {
		return auditor.action("search").readOnly().addActionMetadata("query", query)
				.audit(() -> calendar.search(query));
	}

	@Override
	public ContainerChangeset<String> sync(Long since, VEventChanges changes) throws ServerFault {
		return auditor.action("sync").addActionMetadata("since", since).audit(() -> calendar.sync(since, changes));
	}

	@Override
	public ListResult<ItemValue<VEventSeries>> list() throws ServerFault {
		return auditor.action("list").readOnly().audit(() -> calendar.list());

	}

	@Override
	public TaskRef reset() throws ServerFault {
		return auditor.action("reset").audit(() -> calendar.reset());
	}

	@Override
	public List<String> all() throws ServerFault {
		return auditor.action("all").readOnly().audit(() -> calendar.all());
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		return auditor.action("filteredChangesetById").readOnly().addActionMetadata("since", since)
				.addActionMetadata("filter", filter).audit(() -> calendar.filteredChangesetById(since, filter));
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		return auditor.action("count").readOnly().addActionMetadata("filter", filter)
				.audit(() -> calendar.count(filter));
	}

	@Override
	public Ack updateById(long id, VEventSeries value) {
		return auditor.actionUpdateOn(Long.toString(id)).addActionMetadata("byId", Boolean.TRUE).actionValue(value)
				.withSendNotification(false).audit(() -> calendar.updateById(id, value));
	}

	@Override
	public void deleteById(long id) {
		auditor.actionDeleteOn(Long.toString(id)).addActionMetadata("byId", Boolean.TRUE).withSendNotification(false)
				.audit(() -> calendar.deleteById(id));
	}

	@Override
	public ItemValue<VEventSeries> getCompleteById(long id) {
		return auditor.action("getCompleteById").readOnly().actionItemUid(Long.toString(id))
				.audit(() -> calendar.getCompleteById(id));
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		return auditor.action("sortedIds").readOnly().audit(() -> calendar.sortedIds(sorted));
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {
		auditor.action("xfer").audit(() -> calendar.xfer(serverUid));
	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		auditor.actionDeleteOn("multiple").addActionMetadata("byId", Boolean.TRUE).withSendNotification(false)
				.audit(() -> calendar.multipleDeleteById(ids));
	}

	@Override
	public boolean isAutoSyncActivated() throws ServerFault {
		return auditor.action("isAutoSyncActivated").readOnly().audit(() -> calendar.isAutoSyncActivated());
	}

	@Override
	public ContainerUpdatesResult updates(VEventChanges changes, boolean notify) {
		return calendar.updates(changes, notify);
	}

	@Override
	public void emitNotification() {
		calendar.emitNotification();
	}

	@Override
	public List<ItemValue<VEventSeries>> multipleGetById(List<Long> ids) throws ServerFault {
		return auditor.action("multipleGetById").readOnly().addActionMetadata("itemIds", ids)
				.audit(() -> calendar.multipleGetById(ids));
	}

}

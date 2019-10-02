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
package net.bluemind.calendar.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.EventBus;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.Reminder;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.calendar.persistance.VEventIndexStore;
import net.bluemind.calendar.persistance.VEventSeriesStore;
import net.bluemind.calendar.service.cache.PendingEventsCache;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerSettingsStore;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ContainerSyncStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;

public class CalendarService implements ICalendar {

	private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

	/** When this limit is reached, sync on demand stops. */
	public static final int SYNC_ERRORS_LIMIT = 4;

	private VEventContainerStoreService storeService;
	private VEventIndexStore indexStore;
	private VEventSanitizer sanitizer;
	private Container container;
	private VEventSeriesStore veventStore;
	private CalendarEventProducer calendarEventProducer;
	private Sanitizer extSanitizer;
	private Validator extValidator;

	private BmContext context;

	private VEventValidator validator;

	private RBACManager rbacManager;

	private CalendarAuditor auditor;

	public CalendarService(DataSource pool, Client esearchClient, Container container, BmContext context,
			CalendarAuditor auditor) throws ServerFault {
		if (pool.equals(context.getDataSource())) {
			throw new ServerFault("wrong datasource");
		}
		this.container = container;
		this.context = context;
		this.auditor = auditor;
		sanitizer = new VEventSanitizer(context, container.domainUid);

		veventStore = new VEventSeriesStore(pool, container);
		storeService = new VEventContainerStoreService(context, pool, context.getSecurityContext(), container,
				"calendar", veventStore);

		indexStore = new VEventIndexStore(esearchClient, container);

		EventBus eventBus = VertxPlatform.eventBus();
		calendarEventProducer = new CalendarEventProducer(auditor, container, context.getSecurityContext(), eventBus);

		final String origin = context.getSecurityContext().getOrigin();
		final boolean isRemote = this.isRemoteCalendar(context, container);
		calendarEventProducer.serviceAccessed(container.uid, origin, isRemote);

		extSanitizer = new Sanitizer(context);
		extValidator = new Validator(context);
		validator = new VEventValidator();
		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	private boolean isRemoteCalendar(final BmContext context, final Container container) {
		final ContainerSettingsStore containerSettingsStore = new ContainerSettingsStore(
				DataSourceRouter.get(context, container.uid), container);
		Map<String, String> settings;
		try {
			settings = containerSettingsStore.getSettings();
			if (settings != null && settings.containsKey("icsUrl")) {
				return true;
			}
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
		return false;
	}

	@Override
	public void create(String uid, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		create(uid, null, event, sendNotifications);
	}

	@Override
	public Ack createById(long id, VEventSeries event) throws ServerFault {
		String uid = "vevent-by-id:" + id;
		long version = create(uid, id, event, false);
		return Ack.create(version);
	}

	private long create(String uid, Long internalId, VEventSeries event, Boolean sendNotifications) {
		rbacManager.check(Verb.Write.name());

		if (event == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		if (sendNotifications == null) {
			sendNotifications = false;
		}

		long version = doCreate(uid, internalId, event, sendNotifications);

		// FIXME we should not call refresh
		indexStore.refresh();
		calendarEventProducer.changed();

		return version;
	}

	private long doCreate(String uid, Long internalId, VEventSeries event, boolean sendNotifications)
			throws ServerFault {
		rbacManager.check(Verb.Write.name());

		if (StringUtils.isEmpty(event.icsUid)) {
			event.icsUid = uid;
		}

		sanitizer.sanitize(event);
		extSanitizer.create(event);

		auditor.actionValueSanitized(event);

		validator.create(event);
		extValidator.create(event);

		String summary = event.displayName();
		ItemVersion version = storeService.createWithId(uid, internalId, null, summary, event);
		indexStore.create(uid, event);
		calendarEventProducer.veventCreated(event, uid, sendNotifications);

		return version.version;
	}

	private void doCreateOrUpdate(String uid, VEventSeries event, boolean sendNotification) throws ServerFault {
		try {
			doCreate(uid, null, event, sendNotification);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("Event uid {} was sent as created but already exists. We update it", uid);
				doUpdate(uid, null, event, sendNotification);
			} else {
				throw sf;
			}
		}
	}

	private void doUpdateOrCreate(String uid, VEventSeries event, boolean sendNotification) throws ServerFault {
		try {
			doUpdate(uid, null, event, sendNotification);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("Event uid {} was sent as created but already exists. We update it", uid);
				doCreate(uid, null, event, sendNotification);
			} else {
				throw sf;
			}
		}
	}

	@Override
	public Ack updateById(long id, VEventSeries event) {
		rbacManager.check(Verb.Write.name());

		if (event == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		ItemVersion upd = doUpdate(null, id, event, false);

		// FIXME we should not call refresh
		indexStore.refresh();
		calendarEventProducer.changed();
		return Ack.create(upd.version);
	}

	@Override
	public void update(String uid, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		if (event == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		if (sendNotifications == null) {
			sendNotifications = false;
		}

		doUpdate(uid, null, event, sendNotifications);

		// FIXME we should not call refresh
		indexStore.refresh();
		calendarEventProducer.changed();

	}

	private ItemVersion doUpdate(String optUid, Long itemId, VEventSeries event, Boolean sendNotifications)
			throws ServerFault {
		ItemValue<VEventSeries> old = itemId != null ? storeService.get(itemId, null) : storeService.get(optUid, null);
		if (old == null) {
			throw ServerFault.notFound("entry[" + optUid + "/" + itemId + "]@" + container.uid + " not found");
		}
		String uid = old.uid;

		auditor.previousValue(old.value);
		if ((old.value != null && old.value.main != null
				&& (old.value.main.classification == Classification.Confidential
						|| old.value.main.classification == Classification.Private))
				&& !rbacManager.can(Verb.All.name())) {
			throw new ServerFault("cannot modify private event", ErrorCode.PERMISSION_DENIED);
		}

		if (event.icsUid == null) {
			event.icsUid = old.value.icsUid;
		}
		if (!old.value.icsUid.equals(event.icsUid)) {
			logger.error("ics uid was {} and is now {}", old.value.icsUid, event.icsUid);
			throw new ServerFault("cannot modify ics uid", ErrorCode.INVALID_PARAMETER);
		}
		sanitizer.sanitize(event);
		extSanitizer.update(old.value, event);

		auditor.actionValueSanitized(event);
		validator.update(old.value, event);
		extValidator.update(old.value, event);

		if (event.properties == null) {
			event.properties = old.value.properties;
		}

		String summary = event.displayName();
		ItemVersion upd = storeService.update(uid, summary, event);
		indexStore.update(uid, event);
		calendarEventProducer.veventUpdated(old.value, event, uid, sendNotifications);
		return upd;
	}

	@Override
	public ItemValue<VEventSeries> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ItemValue<VEventSeries> ret = getItemValue(uid);
		return ret;
	}

	@Override
	public ItemValue<VEventSeries> getCompleteById(long id) {
		rbacManager.check(Verb.Read.name());

		ItemValue<VEventSeries> ret = storeService.get(id, null);
		return filter(ret);
	}

	public List<ItemValue<VEventSeries>> getByIcsUid(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		List<ItemValue<VEventSeries>> values = storeService.getByIcsUid(uid);
		return filterValues(values);
	}

	private ItemValue<VEventSeries> getItemValue(String uid) throws ServerFault {
		ItemValue<VEventSeries> ret = storeService.get(uid, null);
		return filter(ret);
	}

	private ItemValue<VEventSeries> filter(ItemValue<VEventSeries> ret) {
		if (ret == null || ret.value == null) {
			return null;
		}

		if (ret.value.main != null && ret.value.main.classification != Classification.Public
				&& !rbacManager.can(Verb.All.name())) {
			ret.value.main = ret.value.main.filtered();
			ret.value.occurrences = ret.value.occurrences.stream().map(occurrence -> {
				return occurrence.filtered();
			}).collect(Collectors.toList());
		}

		return ret;
	}

	@Override
	public List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		List<ItemValue<VEventSeries>> values = storeService.getMultiple(uids);

		return filterValues(values);
	}

	private List<ItemValue<VEventSeries>> filterValues(List<ItemValue<VEventSeries>> values) throws ServerFault {

		for (ItemValue<VEventSeries> value : values) {
			value = filter(value);
		}
		return values;
	}

	@Override
	public void deleteById(long id) {
		rbacManager.check(Verb.Write.name());

		doDelete(null, id, false);

		// FIXME we should not call refresh
		indexStore.refresh();

		calendarEventProducer.changed();
	}

	@Override
	public void delete(String uid, Boolean sendNotifications) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		doDelete(uid, null, sendNotifications);

		// FIXME we should not call refresh
		indexStore.refresh();

		calendarEventProducer.changed();
	}

	private void doDelete(String optUid, Long itemId, Boolean sendNotifications) throws ServerFault {
		ItemValue<VEventSeries> item = itemId != null ? storeService.get(itemId, null) : storeService.get(optUid, null);

		if (item == null) {
			throw ServerFault.notFound("entry[" + optUid + "/" + itemId + "]@" + container.uid + " not found");
		}
		String uid = item.uid;

		auditor.previousValue(item.value);
		storeService.delete(uid);
		indexStore.delete(uid);

		calendarEventProducer.veventDeleted(item.value, uid, sendNotifications);

	}

	@Override
	public void touch(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		storeService.touch(uid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		if (changes == null) {
			return ret;
		}

		boolean changed = false;
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();
		try {

			if (changes.add != null) {
				for (VEventChanges.ItemAdd item : changes.add) {

					try {
						auditor.actionCreateOn(item.uid).readOnly(false).actionValue(item.value)
								.withSendNotification(item.sendNotification)
								.audit(() -> doCreateOrUpdate(item.uid, item.value, item.sendNotification));
						changed = true;
						ret.added.add(item.uid);
					} catch (ServerFault sf) {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
						logger.error(sf.getMessage(), sf);
					}

				}
			}

			if (changes.modify != null) {
				for (VEventChanges.ItemModify item : changes.modify) {
					try {
						auditor.actionUpdateOn(item.uid).readOnly(false).actionValue(item.value)
								.withSendNotification(item.sendNotification)
								.audit(() -> doUpdateOrCreate(item.uid, item.value, item.sendNotification));
						changed = true;
						ret.updated.add(item.uid);
					} catch (ServerFault sf) {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
						logger.error(sf.getMessage(), sf);
					}
				}
			}

			if (changes.delete != null) {
				for (VEventChanges.ItemDelete item : changes.delete) {
					try {
						auditor.actionDeleteOn(item.uid).readOnly(false).withSendNotification(item.sendNotification)
								.audit(() -> doDelete(item.uid, null, item.sendNotification));
						changed = true;
						ret.removed.add(item.uid);
					} catch (ServerFault sf) {
						if (sf.getCode() == ErrorCode.NOT_FOUND) {
							logger.warn("Event uid {} was sent as deleted but does not exist.", item.uid);
							ret.removed.add(item.uid);
						} else {
							ret.errors.add(
									ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
							logger.error(sf.getMessage(), sf);
						}
					}

				}
			}

		} finally {
			if (changed) {
				indexStore.refresh();
				calendarEventProducer.changed();
			}
		}
		ret.version = storeService.getVersion();
		return ret;

	}

	@Override
	public ListResult<ItemValue<VEventSeries>> search(VEventQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		ListResult<ItemValue<VEventSeries>> ret = new ListResult<>();

		List<ItemValue<VEventSeries>> items;
		if (isPendingEventsSearch(query)) {
			ListResult<ItemValue<VEventSeries>> res = searchPendingEvents(query);
			items = filterValues(((ListResult<ItemValue<VEventSeries>>) res).values.stream().map(iv -> {
				return ItemValue.create(iv, iv.value.copy());
			}).collect(Collectors.toList()));
			ret.total = res.total;
		} else {
			if (query.attendee != null && query.attendee.calendarOwnerAsDir) {
				addOwnerToQuery(query);
			}
			ListResult<String> res = indexStore.search(query, searchInPrivate());
			items = filterValues(storeService.getMultiple(res.values));
			ret.total = res.total;
		}

		ret.values = items;
		return ret;
	}

	private boolean isPendingEventsSearch(VEventQuery query) {
		return query.attendee != null && query.attendee.calendarOwnerAsDir && query.dateMin != null
				&& query.attendee.partStatus == ParticipationStatus.NeedsAction;
	}

	private ListResult<ItemValue<VEventSeries>> searchPendingEvents(VEventQuery query) {
		ListResult<ItemValue<VEventSeries>> res = PendingEventsCache.getIfPresent(container.uid);
		if (res == null) {
			addOwnerToQuery(query);
			ListResult<String> t = indexStore.search(query, searchInPrivate());
			List<ItemValue<VEventSeries>> items = storeService.getMultiple(t.values);
			res = ListResult.create(items);
			PendingEventsCache.put(container.uid, res);
		} else {
			res.values = res.values.stream().filter(vEventSeries -> {
				VEvent main = vEventSeries.value.mainOccurrence();

				if (main.hasRecurrence()) {
					return main.rrule.until == null || new BmDateTimeWrapper(main.rrule.until).isAfter(query.dateMin);
				}

				return new BmDateTimeWrapper(main.dtend).isAfter(query.dateMin);
			}).collect(Collectors.toList());
		}
		return res;
	}

	private void addOwnerToQuery(VEventQuery query) {
		DirEntry dirEntry = context.su().provider().instance(IDirectory.class, container.domainUid)
				.findByEntryUid(container.owner);
		query.attendee.dir = "bm://" + dirEntry.path;
	}

	private boolean searchInPrivate() {
		return context.getSecurityContext().isDomainGlobal()
				|| context.getSecurityContext().getSubject().equals(container.owner);
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
		return storeService.changesetById(since, filter);
	}

	@Override
	public ContainerChangeset<String> sync(Long since, VEventChanges changes) throws ServerFault {
		updates(changes);
		return changeset(since);
	}

	@Override
	public ListResult<ItemValue<VEventSeries>> list() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		List<String> allUids = storeService.allUids();

		List<ItemValue<VEventSeries>> values = new ArrayList<>(allUids.size());

		for (String uid : allUids) {
			ItemValue<VEventSeries> evt = getItemValue(uid);
			if (evt != null) {
				sanitizer.resolveAttendeesAndOrganizer(evt.value);
			}
			values.add(evt);
		}

		ListResult<ItemValue<VEventSeries>> ret = new ListResult<>();
		ret.total = values.size();
		ret.values = values;

		return ret;
	}

	@Override
	public List<String> all() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public TaskRef reset() throws ServerFault {
		rbacManager.check(Verb.Manage.name());

		return context.provider().instance(ITasksManager.class).run(new IServerTask() {

			@Override
			public void run(IServerTaskMonitor monitor) throws Exception {
				monitor.begin(2, String.format("reset calendar %s ... ", container.uid));

				storeService.deleteAll();
				monitor.progress(1, String.format("reset calendar %s, delete events from store", container.uid));

				indexStore.deleteAll();
				monitor.progress(1, String.format("reset calendar %s, delete events from index", container.uid));

				calendarEventProducer.changed();

				ContainerSyncStatus status = new ContainerSyncStatus();
				status.nextSync = 0L;
				new ContainerSyncStore(context.getDataSource(), container).setSyncStatus(status);

				monitor.end(true, String.format("reset calendar %s done ", container.uid), "[]");
			}

		});
	}

	@Override
	public List<Reminder> getReminder(BmDateTime dtalarm) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		List<String> events = storeService.getReminder(dtalarm);

		logger.debug("Found {} event candidates for reminder email", events.size());

		List<Reminder> ret = new LinkedList<>();
		Set<String> eventUids = new HashSet<>();
		for (String uid : events) {

			ItemValue<VEventSeries> item = storeService.get(uid, null);
			if (eventUids.contains(item.uid)) {
				continue;
			}

			eventUids.add(item.uid);
			ItemValue<VEventSeries> evt = storeService.get(item.uid, null);

			VEvent event = evt.value.main;
			if (event != null && event.alarm != null && event.alarm.size() > 0) {
				logger.debug("Event {} has {} alarms", uid, event.alarm.size());
				if (event.rrule == null) {
					logger.debug("Event {} has no RRule", uid);
					for (VAlarm valarm : event.alarm) {
						BmDateTime expected = BmDateTimeWrapper.fromTimestamp(
								new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
								event.dtstart.timezone);
						logger.debug("Comparing Expected {} to events dtstart {}", expected.toString(),
								event.dtstart.toString());
						if (expected.equals(event.dtstart)) {
							ret.add(Reminder.create(ItemValue.create(evt, event), valarm));
						}
					}
				} else {
					logger.debug("Event {} has occurrences", uid);
					for (VAlarm valarm : event.alarm) {
						VEvent occurrence = OccurrenceHelper.getOccurrence(evt,
								BmDateTimeWrapper.fromTimestamp(
										new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
										event.dtstart.timezone));

						if (occurrence != null) {
							ret.add(Reminder.create(ItemValue.create(evt, occurrence), valarm));
						}
					}
				}
			}

		}

		logger.debug("Returning {} reminders to client", ret.size());
		return ret;
	}

	@Override
	public long getVersion() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public Count count(ItemFlagFilter filter) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.count(filter);
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		try {
			return veventStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
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

		storeService.xfer(ds, c, new VEventSeriesStore(ds, c));

	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		ids.forEach(id -> doDelete(null, id, false));

		// FIXME we should not call refresh
		indexStore.refresh();

		calendarEventProducer.changed();
	}

	@Override
	public boolean isAutoSyncActivated() throws ServerFault {
		final ContainerSyncStore containerSyncStore = new ContainerSyncStore(
				DataSourceRouter.get(context, container.uid), container);
		final ContainerSyncStatus containerSyncStatus = containerSyncStore.getSyncStatus();
		return containerSyncStatus != null ? containerSyncStatus.errors < SYNC_ERRORS_LIMIT : true;
	}

}

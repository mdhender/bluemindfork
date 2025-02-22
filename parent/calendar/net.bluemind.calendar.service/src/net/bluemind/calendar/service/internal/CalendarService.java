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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.auditlog.CalendarAuditor;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.VEventMessage;
import net.bluemind.calendar.persistence.VEventIndexStore;
import net.bluemind.calendar.persistence.VEventSeriesStore;
import net.bluemind.calendar.service.cache.PendingEventsCache;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.validator.Validator;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.lib.vertx.VertxPlatform;

public class CalendarService implements IInternalCalendar {

	private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

	/** When this limit is reached, sync on demand stops. */
	public static final int SYNC_ERRORS_LIMIT = 4;

	private static final List<ICalendarHook> syncHooks = loadHooks(true);
	private static final List<ICalendarHook> asyncHooks = loadHooks(false);

	private VEventContainerStoreService storeService;
	private VEventIndexStore indexStore;
	private Supplier<VEventSanitizer> sanitizer;
	private Container container;
	private VEventSeriesStore veventStore;
	private CalendarEventProducer calendarEventProducer;
	private Sanitizer extSanitizer;
	private Validator extValidator;

	private BmContext context;
	private final Vertx vertx;

	private VEventValidator validator;

	private RBACManager rbacManager;

	private CalendarAuditor auditor;

	public CalendarService(DataSource pool, ElasticsearchClient esClient, Container container, BmContext context,
			CalendarAuditor auditor, VEventContainerStoreService storeService) throws ServerFault {
		this.container = container;
		this.context = context;
		this.auditor = auditor;
		this.storeService = storeService;
		sanitizer = Suppliers.memoize(() -> new VEventSanitizer(context, container));

		veventStore = new VEventSeriesStore(pool, container);

		indexStore = new VEventIndexStore(esClient, container, DataSourceRouter.location(context, container.uid));

		EventBus eventBus = VertxPlatform.eventBus();
		calendarEventProducer = new CalendarEventProducer(container, eventBus);

		final String origin = context.getSecurityContext().getOrigin();
		final boolean isRemote = this.isRemoteCalendar(context, container);
		calendarEventProducer.serviceAccessed(container.uid, origin, context.getSecurityContext().isInteractive(),
				isRemote);

		extSanitizer = new Sanitizer(context, container);
		extValidator = new Validator(context);
		validator = new VEventValidator();
		rbacManager = RBACManager.forContext(context).forContainer(container);
		vertx = VertxPlatform.getVertx();
	}

	private static List<ICalendarHook> loadHooks(boolean synchronous) {
		return new RunnableExtensionLoader<ICalendarHook>() //
				.loadExtensionsWithPriority("net.bluemind.calendar", "hook", "hook", "impl") //
				.stream().filter(hook -> hook.isSynchronous() == synchronous).collect(Collectors.toList());

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
		Item item = Item.create(uid, null);
		create(item, event, sendNotifications);
	}

	@Override
	public Ack createById(long id, VEventSeries event) throws ServerFault {
		Item item = Item.create("vevent-by-id:" + id, id);
		ItemVersion version = create(item, event, false);
		return version.ack();
	}

	private ItemVersion create(Item item, VEventSeries event, Boolean sendNotifications) {
		rbacManager.check(Verb.Write.name());

		if (event == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		if (sendNotifications == null) {
			sendNotifications = false;
		}

		ItemVersion version = doCreate(item, event, sendNotifications);

		emitNotification();

		return version;
	}

	private ItemVersion doCreate(Item item, VEventSeries event, boolean sendNotifications) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		if (Strings.isNullOrEmpty(event.icsUid)) {
			event.icsUid = item.uid;
		}

		sanitizer.get().sanitize(event, sendNotifications);
		extSanitizer.create(event);

		auditor.actionValueSanitized(event);

		validator.create(event);
		extValidator.create(event);

		item.displayName = event.displayName();
		ItemVersion version = storeService.create(item, event);
		indexStore.create(Item.create(item.uid, version.id), event);

		VEventMessage hookEventMsg = asHookEventMessage(item.uid, event, sendNotifications);
		callHooks(hook -> hook.onEventCreated(hookEventMsg));

		return version;
	}

	private void doCreateOrUpdate(String uid, VEventSeries event, boolean sendNotification) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doCreate(item, event, sendNotification);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("Event uid {} was sent as created but already exists. We update it", uid);
				doUpdate(item, event, sendNotification);
			} else {
				throw sf;
			}
		}
	}

	private void doUpdateOrCreate(String uid, VEventSeries event, boolean sendNotification) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doUpdate(item, event, sendNotification);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("Event uid {} was sent as created but already exists. We update it", uid);
				doCreate(item, event, sendNotification);
			} else {
				throw sf;
			}
		}
	}

	@Override
	public Ack updateById(long id, VEventSeries event) {
		Item item = Item.create(null, id);
		ItemVersion version = update(item, event, false);
		return version.ack();
	}

	@Override
	public void update(String uid, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		Item item = Item.create(uid, null);
		update(item, event, sendNotifications);
	}

	private ItemVersion update(Item item, VEventSeries event, Boolean sendNotifications) {
		rbacManager.check(Verb.Write.name());
		if (event == null) {
			throw new ServerFault("VEvent is null", ErrorCode.EVENT_ERROR);
		}

		if (sendNotifications == null) {
			sendNotifications = false;
		}

		ItemVersion upd = doUpdate(item, event, sendNotifications);

		emitNotification();
		return upd;
	}

	private ItemVersion doUpdate(Item item, VEventSeries event, Boolean sendNotifications) throws ServerFault {
		ItemValue<VEventSeries> old = item.uid == null ? storeService.get(item.id, null)
				: storeService.get(item.uid, null);
		if (old == null || old.value == null) {
			throw ServerFault.notFound("entry[" + item.uid + "/" + item.id + "]@" + container.uid + " not found");
		}
		item.uid = old.uid;

		VEventCancellationSanitizer.sanitize(old.value, event);

		auditor.previousValue(old.value);
		if ((old.value != null && old.value.main != null
				&& (old.value.main.classification == Classification.Confidential
						|| old.value.main.classification == Classification.Private))
				&& !rbacManager.can(Verb.Write.name(), Verb.ReadExtended.name())) {
			throw new ServerFault("cannot modify private event", ErrorCode.PERMISSION_DENIED);
		}

		if (event.icsUid == null) {
			event.icsUid = old.value.icsUid;
		}

		if (!old.value.icsUid.equals(event.icsUid)) {
			logger.error("ics uid was {} and is now {}", old.value.icsUid, event.icsUid);
			throw new ServerFault("cannot modify ics uid", ErrorCode.INVALID_PARAMETER);
		}
		sanitizer.get().sanitize(event, sendNotifications);
		extSanitizer.update(old.value, event);

		auditor.actionValueSanitized(event);
		validator.update(old.value, event);
		extValidator.update(old.value, event);

		if (event.properties == null) {
			event.properties = old.value.properties;
		}

		ItemVersion upd = storeService.update(item, event.displayName(), event);
		indexStore.update(Item.create(item.uid, upd.id), event);

		VEventMessage hookEventMsg = asHookEventMessage(item.uid, event, old.value, sendNotifications);
		callHooks(hook -> hook.onEventUpdated(hookEventMsg));
		return upd;
	}

	@Override
	public ItemValue<VEventSeries> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		return getItemValue(uid);
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
				&& !rbacManager.can(Verb.ReadExtended.name())) {
			ret.value.main = ret.value.main.filtered();
			ret.value.occurrences = ret.value.occurrences.stream().map(VEventOccurrence::filtered)
					.collect(Collectors.toList());
		}

		return ret;
	}

	@Override
	public List<ItemValue<VEventSeries>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		List<ItemValue<VEventSeries>> values = storeService.getMultiple(uids);

		return filterValues(values);
	}

	@Override
	public List<ItemValue<VEventSeries>> multipleGetById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		List<ItemValue<VEventSeries>> values = storeService.getMultipleById(ids);

		return filterValues(values);
	}

	private List<ItemValue<VEventSeries>> filterValues(List<ItemValue<VEventSeries>> values) throws ServerFault {
		values.forEach(this::filter);
		return values;
	}

	@Override
	public void deleteById(long id) {
		rbacManager.check(Verb.Write.name());

		doDelete(null, id, false);

		emitNotification();
	}

	@Override
	public void delete(String uid, Boolean sendNotifications) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		doDelete(uid, null, sendNotifications);

		emitNotification();
	}

	private void doDelete(String optUid, Long itemId, Boolean sendNotifications) {
		ItemValue<VEventSeries> item = itemId != null ? storeService.get(itemId, null) : storeService.get(optUid, null);

		if (item == null) {
			logger.warn("Failed to delete, event not found {}/{}", optUid, itemId);
			return;
		}

		auditor.previousValue(item.value);
		storeService.delete(item.uid);
		indexStore.delete(item.internalId);

		VEventMessage hookEventMsg = asHookEventMessage(item.uid, item.value, sendNotifications);
		callHooks(hook -> hook.onEventDeleted(hookEventMsg));
	}

	@Override
	public void touch(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		storeService.touch(uid);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, container);
	}

	@Override
	public ContainerUpdatesResult updates(VEventChanges changes, boolean notify) {
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
							logger.warn("Event uid {} was sent as deleted but does not exist.", item.uid);
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
			if (changed && notify) {
				emitNotification();
			}
		}
		ret.version = storeService.getVersion();
		return ret;

	}

	@Override
	public ContainerUpdatesResult updates(VEventChanges changes) throws ServerFault {
		return updates(changes, true);
	}

	@Override
	public ListResult<ItemValue<VEventSeries>> search(VEventQuery query) {
		rbacManager.check(Verb.Read.name());
		ListResult<ItemValue<VEventSeries>> ret = new ListResult<>();
		List<ItemValue<VEventSeries>> items;

		try {
			if (isPendingEventsSearch(query)) {
				ListResult<ItemValue<VEventSeries>> res;
				res = searchPendingEvents(query);
				items = filterValues(res.values.stream() //
						.map(iv -> ItemValue.create(iv, iv.value.copy())).collect(Collectors.toList()));
				ret.total = res.total;
			} else {
				if (query.attendee != null && query.attendee.calendarOwnerAsDir) {
					Optional<DirEntry> owner = getCalendarOwner();
					owner.ifPresent(de -> query.attendee.dir = "bm://" + de.path);
				}
				ListResult<String> res;
				res = indexStore.search(query, searchInPrivate());
				items = filterValues(storeService.getMultiple(res.values));
				ret.total = res.total;
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}

		ret.values = items;
		return ret;
	}

	private boolean isPendingEventsSearch(VEventQuery query) {
		return query.attendee != null && query.attendee.calendarOwnerAsDir && query.dateMin != null
				&& query.attendee.partStatus == ParticipationStatus.NeedsAction;
	}

	private ListResult<ItemValue<VEventSeries>> searchPendingEvents(VEventQuery query) throws Exception {
		ListResult<ItemValue<VEventSeries>> res = PendingEventsCache.getIfPresent(container.uid);
		if (res == null) {
			Optional<DirEntry> owner = getCalendarOwner();
			if (!owner.isPresent()) {
				return ListResult.create(Collections.emptyList());
			}
			query.attendee.dir = "bm://" + owner.get().path;
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
		res.values = res.values.stream().map(series -> {
			if (series.value.occurrences != null && !series.value.occurrences.isEmpty()) {
				series.value.occurrences = series.value.occurrences.stream()
						.filter(occ -> new BmDateTimeWrapper(occ.dtend).isAfter(query.dateMin))
						.collect(Collectors.toList());
			}
			return series;
		}).collect(Collectors.toList());
		return res;
	}

	private Optional<DirEntry> getCalendarOwner() {
		return Optional.ofNullable(context.su().provider().instance(IDirectory.class, container.domainUid)
				.findByEntryUid(container.owner));
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
			values.add(getItemValue(uid));
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

		return context.provider().instance(ITasksManager.class).run(new BlockingServerTask() {

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

		emitNotification();
	}

	@Override
	public boolean isAutoSyncActivated() throws ServerFault {
		return true;
	}

	@Override
	public void emitNotification() {
		indexStore.refresh();
		calendarEventProducer.changed();
	}

	@Override
	public ListResult<ItemValue<VEventSeries>> searchPendingCounters() {
		Optional<DirEntry> owner = getCalendarOwner();
		if (!owner.isPresent()) {
			return ListResult.create(Collections.emptyList());
		}
		String path = "bm://" + owner.get().path;
		List<ItemValue<VEventSeries>> pendingPropositions = storeService.searchPendingPropositions(path);
		pendingPropositions = pendingPropositions.stream().filter(vEventSeries -> {
			VEvent main = vEventSeries.value.mainOccurrence();

			BmDateTime now = BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis());
			if (main.hasRecurrence()) {
				return main.rrule.until == null || new BmDateTimeWrapper(main.rrule.until).isAfter(now);
			}

			return new BmDateTimeWrapper(main.dtend).isAfter(now);
		}).collect(Collectors.toList());
		return ListResult.create(pendingPropositions);
	}

	@Override
	public VEventSeries get(String uid) {
		ItemValue<VEventSeries> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<VEventSeries> eventItem, boolean isCreate) {
		if (isCreate) {
			create(eventItem.item(), eventItem.value, false);
		} else {
			update(eventItem.item(), eventItem.value, false);
		}
	}

	@Override
	public void delete(String uid) {
		delete(uid, false);
	}

	private void callHooks(Consumer<ICalendarHook> hookCallback) {
		syncHooks.forEach(hookCallback::accept);
		vertx.executeBlocking(() -> {
			asyncHooks.stream().forEach(hookCallback::accept);
			return null;
		}, false);
	}

	private VEventMessage asHookEventMessage(String uid, VEventSeries event, boolean notifications) {
		return asHookEventMessage(uid, event, null, notifications);
	}

	private VEventMessage asHookEventMessage(String uid, VEventSeries event, VEventSeries previous,
			boolean notifications) {
		VEventMessage message = new VEventMessage(event, uid, notifications, context.getSecurityContext(),
				auditor.eventId(), container);
		message.oldEvent = previous;
		return message;
	}
}

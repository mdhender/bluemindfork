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
package net.bluemind.todolist.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.Reminder;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoQuery;
import net.bluemind.todolist.persistance.VTodoIndexStore;
import net.bluemind.todolist.persistance.VTodoStore;
import net.bluemind.todolist.service.helper.OccurrenceHelper;

public class TodoListService implements ITodoList {

	private static final Logger logger = LoggerFactory.getLogger(TodoListService.class);
	private VTodoContainerStoreService storeService;
	private VTodoIndexStore indexStore;
	private VTodoSanitizer sanitizer;
	private VTodoValidator validator;
	private TodoListEventProducer eventProducer;
	private SecurityContext context;
	private Sanitizer extSanitizer;
	private Validator extValidator;
	private BmContext bmContext;
	private Container container;
	private RBACManager rbacManager;
	private VTodoStore vtodoStore;

	public TodoListService(DataSource pool, Client esearchClient, Container container, BmContext bmContext) {
		if (!container.owner.equals(container.domainUid) && pool.equals(bmContext.getDataSource())) {
			throw new ServerFault("wrong datasource");
		}
		this.bmContext = bmContext;
		this.container = container;
		this.vtodoStore = new VTodoStore(pool, container);

		storeService = new VTodoContainerStoreService(bmContext, pool, bmContext.getSecurityContext(), container,
				ITodoUids.TYPE, vtodoStore);

		indexStore = new VTodoIndexStore(esearchClient, container);

		eventProducer = new TodoListEventProducer(container, bmContext.getSecurityContext(), VertxPlatform.eventBus());

		sanitizer = new VTodoSanitizer();

		validator = new VTodoValidator();
		context = bmContext.getSecurityContext();

		extSanitizer = new Sanitizer(bmContext);
		extValidator = new Validator(bmContext);
		rbacManager = RBACManager.forContext(bmContext).forContainer(container);
	}

	@Override
	public List<ItemValue<VTodo>> all() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.all();
	}

	@Override
	public void create(String uid, VTodo todo) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		doCreate(uid, todo);
		eventProducer.vtodoCreated(uid, todo);
		eventProducer.changed();
		indexStore.refresh();

	}

	private void doCreate(String uid, VTodo todo) throws ServerFault {
		sanitizer.sanitize(todo);
		extSanitizer.create(todo);
		validator.validate(todo);
		extValidator.create(todo);
		storeService.create(uid, todo.summary, todo);
		indexStore.create(uid, todo);
	}

	private void doCreateOrUpdate(String uid, VTodo todo) throws ServerFault {

		try {
			doCreate(uid, todo);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("task uid {} was sent as created but already exists. We update it", uid);
				doUpdate(uid, todo);
			} else {
				throw sf;
			}
		}

	}

	@Override
	public void update(String uid, VTodo todo) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VTodo> previous = doUpdate(uid, todo);
		eventProducer.vtodoUpdated(uid, previous.value, todo);
		eventProducer.changed();
		indexStore.refresh();
	}

	private ItemValue<VTodo> doUpdate(String uid, VTodo todo) throws ServerFault {
		ItemValue<VTodo> previousItemValue = storeService.get(uid, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			throw new ServerFault("VTodo uid:" + uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}

		sanitizer.sanitize(todo);
		extSanitizer.update(previousItemValue.value, todo);

		validator.validate(todo);
		extValidator.update(previousItemValue.value, todo);

		storeService.update(uid, todo.summary, todo);
		indexStore.update(uid, todo);
		return previousItemValue;
	}

	private void doUpdateOrCreate(String uid, VTodo todo) throws ServerFault {
		try {
			doUpdate(uid, todo);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("task uid {} was sent as created but already exists. We update it", uid);
				doCreate(uid, todo);
			} else {
				throw sf;
			}
		}
	}

	@Override
	public ItemValue<VTodo> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.get(uid, null);
	}

	@Override
	public List<ItemValue<VTodo>> multipleGet(List<String> uids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultiple(uids);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VTodo> previousItemValue = doDelete(uid);
		if (previousItemValue != null) {
			eventProducer.vtodoDeleted(uid, previousItemValue.value);
			eventProducer.changed();
			indexStore.refresh();
		}
	}

	private ItemValue<VTodo> doDelete(String uid) throws ServerFault {
		ItemValue<VTodo> previousItemValue = storeService.get(uid, null);
		storeService.delete(uid);
		indexStore.delete(uid);
		return previousItemValue;
	}

	@Override
	public ListResult<ItemValue<VTodo>> search(VTodoQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ListResult<String> res = indexStore.search(query);

		List<ItemValue<VTodo>> values = new ArrayList<ItemValue<VTodo>>(res.values.size());

		for (String uid : res.values) {
			ItemValue<VTodo> item = getComplete(uid);
			if (item.value.rrule != null && (query.dateMin != null || query.dateMax != null)) {
				List<VTodo> occurrences = OccurrenceHelper.list(item.value, query.dateMin, query.dateMax);
				for (VTodo occurrence : occurrences) {
					ItemValue<VTodo> i = ItemValue.create(item, occurrence);
					values.add(i);
				}
			} else {
				values.add(item);
			}
		}

		ListResult<ItemValue<VTodo>> ret = new ListResult<>();
		ret.total = res.total;
		ret.values = values;

		return ret;
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
		return storeService.changesetById(since, filter);
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return ChangeLogUtil.getItemChangeLog(itemUid, since, bmContext, storeService, container.domainUid);
	}

	@Override
	public ContainerUpdatesResult updates(VTodoChanges changes) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		boolean change = false;

		ContainerUpdatesResult ret = new ContainerUpdatesResult();
		ret.added = new ArrayList<String>();
		ret.updated = new ArrayList<String>();
		ret.removed = new ArrayList<String>();
		ret.errors = new ArrayList<>();

		if (changes.add != null && changes.add.size() > 0) {
			change = true;
			for (VTodoChanges.ItemAdd add : changes.add) {
				try {
					doCreateOrUpdate(add.uid, add.value);
					ret.added.add(add.uid);
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), add.uid));
					logger.error(sf.getMessage(), sf);
				}
			}
		}

		if (changes.modify != null && changes.modify.size() > 0) {
			change = true;
			for (VTodoChanges.ItemModify update : changes.modify) {
				try {
					doUpdateOrCreate(update.uid, update.value);
					ret.updated.add(update.uid);
				} catch (ServerFault sf) {
					ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), update.uid));
					logger.error(sf.getMessage(), sf);
				}
			}
		}

		if (changes.delete != null && changes.delete.size() > 0) {
			change = true;
			for (VTodoChanges.ItemDelete item : changes.delete) {
				try {
					doDelete(item.uid);
					ret.removed.add(item.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("task uid {} was sent as deleted but does not exist.", item.uid);
						ret.removed.add(item.uid);
					} else {
						ret.errors.add(ContainerUpdatesResult.InError.create(sf.getMessage(), sf.getCode(), item.uid));
						logger.error(sf.getMessage(), sf);
					}
				}
			}

		}

		if (change) {
			eventProducer.changed();
			indexStore.refresh();
		}
		ret.version = storeService.getVersion();
		return ret;
	}

	@Override
	public ContainerChangeset<String> sync(Long since, VTodoChanges changes) throws ServerFault {
		if (changes != null) {
			updates(changes);
		}
		return changeset(since);
	}

	@Override
	public void copy(List<String> uids, String descContainerUid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ITodoList dest = ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, descContainerUid);

		// FIXME should use mupdates
		for (String uid : uids) {

			ItemValue<VTodo> value = getComplete(uid);
			if (value != null) {
				dest.create(uid, value.value);
			}
		}

	}

	@Override
	public void move(List<String> uids, String descContainerUid) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		ITodoList dest = ServerSideServiceProvider.getProvider(context).instance(ITodoList.class, descContainerUid);
		// FIXME should use mupdates
		for (String uid : uids) {
			ItemValue<VTodo> value = getComplete(uid);
			if (value != null) {
				dest.create(uid, value.value);
				delete(uid);
			}
		}
	}

	@Override
	public void reset() throws ServerFault {
		rbacManager.check(Verb.Manage.name());
		storeService.deleteAll();
		indexStore.deleteAll();
		eventProducer.changed();
	}

	@Override
	public List<Reminder> getReminder(BmDateTime dtalarm) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		List<String> todos = storeService.getReminder(dtalarm);
		List<Reminder> ret = new LinkedList<>();

		for (String uid : todos) {
			ItemValue<VTodo> item = storeService.get(uid, null);
			VTodo todo = item.value;
			if (todo.alarm != null && todo.alarm.size() > 0) {
				if (todo.rrule == null) {
					for (VAlarm valarm : todo.alarm) {
						BmDateTime expected = BmDateTimeWrapper.fromTimestamp(
								new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
								todo.due.timezone);
						if (expected.equals(todo.dtstart)) {
							ret.add(Reminder.create(null, item, valarm));
						}
					}
				} else {
					for (VAlarm valarm : todo.alarm) {
						VTodo occurrence = OccurrenceHelper.getOccurrence(todo,
								BmDateTimeWrapper.fromTimestamp(
										new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
										todo.due.timezone));

						if (occurrence != null) {
							ret.add(Reminder.create(null, ItemValue.create(item.uid, occurrence), valarm));
						}
					}
				}
			}

		}

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
	public ItemValue<VTodo> getCompleteById(long id) {
		return storeService.get(id, null);
	}

	@Override
	public Ack updateById(long id, VTodo value) {
		ItemVersion upd = storeService.update(id, value.summary, value);
		return Ack.create(upd.version);
	}

	@Override
	public Ack createById(long id, VTodo value) {
		ItemVersion version = storeService.createWithId("todo-by-id-" + id, id, value.uid, value.summary, value);
		return Ack.create(version.version);
	}

	@Override
	public void deleteById(long id) {
		ItemValue<VTodo> prev = getCompleteById(id);
		if (prev != null) {
			delete(prev.uid);
		}
	}

	public List<String> allUids() throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.allUids();
	}

	@Override
	public List<Long> sortedIds(SortDescriptor sorted) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		try {
			return vtodoStore.sortedIds(sorted);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void xfer(String serverUid) throws ServerFault {

		DataSource ds = bmContext.getMailboxDataSource(serverUid);
		ContainerStore cs = new ContainerStore(null, ds, bmContext.getSecurityContext());
		Container c;
		try {
			c = cs.get(container.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		storeService.xfer(ds, c, new VTodoStore(ds, c));

	}

	@Override
	public void multipleDeleteById(List<Long> ids) throws ServerFault {
		ids.forEach(id -> deleteById(id));
	}

}

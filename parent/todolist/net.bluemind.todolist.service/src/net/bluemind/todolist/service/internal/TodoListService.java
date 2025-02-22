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
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.api.VTodoChanges;
import net.bluemind.todolist.api.VTodoQuery;
import net.bluemind.todolist.persistence.VTodoIndexStore;
import net.bluemind.todolist.persistence.VTodoStore;
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

	public TodoListService(DataSource pool, ElasticsearchClient esClient, Container container,
			BmContext bmContext, VTodoContainerStoreService storeService) {
		this.bmContext = bmContext;
		this.container = container;
		this.vtodoStore = new VTodoStore(pool, container);

		this.storeService = storeService;

		indexStore = new VTodoIndexStore(esClient, container, DataSourceRouter.location(bmContext, container.uid));

		eventProducer = new TodoListEventProducer(container, bmContext.getSecurityContext(), VertxPlatform.eventBus());

		sanitizer = new VTodoSanitizer(bmContext, container);

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
		Item item = Item.create(uid, null);
		create(item, todo);
	}

	private Ack create(Item item, VTodo todo) {
		rbacManager.check(Verb.Write.name());
		Ack ack = doCreate(item, todo);
		eventProducer.vtodoCreated(item.uid, todo);
		eventProducer.changed();
		indexStore.refresh();
		return ack;
	}

	private Ack doCreate(Item item, VTodo todo) throws ServerFault {
		sanitizer.sanitize(todo);
		extSanitizer.create(todo);
		validator.validate(todo);
		extValidator.create(todo);
		item.displayName = todo.summary;
		if (todo.uid != null) {
			List<ItemValue<VTodo>> existing = getByIcsUid(todo.uid);
			if (existing != null && !existing.isEmpty()) {
				return existing.get(0).identifier().ack();
			}
		}
		ItemVersion iv = storeService.create(item, todo);
		indexStore.create(Item.create(item.uid, iv.id), todo);
		return iv.ack();
	}

	private void doCreateOrUpdate(String uid, VTodo todo) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doCreate(item, todo);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.ALREADY_EXISTS) {
				logger.warn("task uid {} was sent as created but already exists. We update it", uid);
				doUpdate(item, todo);
			} else {
				throw sf;
			}
		}

	}

	@Override
	public void update(String uid, VTodo todo) throws ServerFault {
		Item item = Item.create(uid, null);
		doUpdate(item, todo);
	}

	private ItemVersion doUpdate(Item item, VTodo todo) throws ServerFault {
		rbacManager.check(Verb.Write.name());
		ItemValue<VTodo> previousItemValue = item.uid == null ? storeService.get(item.id, null)
				: storeService.get(item.uid, null);
		if (previousItemValue == null || previousItemValue.value == null) {
			throw new ServerFault("VTodo uid:" + item.uid + " doesn't exist !", ErrorCode.NOT_FOUND);
		}
		item.uid = previousItemValue.uid;

		sanitizer.sanitize(previousItemValue, todo);
		extSanitizer.update(previousItemValue.value, todo);

		validator.validate(todo);
		extValidator.update(previousItemValue.value, todo);

		ItemVersion version = storeService.update(item, todo.summary, todo);
		indexStore.update(Item.create(item.uid, previousItemValue.internalId), todo);

		eventProducer.vtodoUpdated(item.uid, previousItemValue.value, todo);
		eventProducer.changed();
		indexStore.refresh();
		return version;
	}

	private void doUpdateOrCreate(String uid, VTodo todo) throws ServerFault {
		Item item = Item.create(uid, null);
		try {
			doUpdate(item, todo);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				logger.warn("task uid {} was sent as created but already exists. We update it", uid);
				doCreate(item, todo);
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
	public List<ItemValue<VTodo>> multipleGetById(List<Long> ids) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getMultipleById(ids);
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
		if (previousItemValue != null) {
			storeService.delete(uid);
			indexStore.delete(previousItemValue.internalId);
			return previousItemValue;
		}
		return null;
	}

	@Override
	public ListResult<ItemValue<VTodo>> search(VTodoQuery query) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ListResult<String> res = indexStore.search(query);

		List<ItemValue<VTodo>> values = new ArrayList<>(res.values.size());

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
		return ChangeLogUtil.getItemChangeLog(itemUid, since, bmContext, container);
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

		if (changes.add != null && !changes.add.isEmpty()) {
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

		if (changes.modify != null && !changes.modify.isEmpty()) {
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

		if (changes.delete != null && !changes.delete.isEmpty()) {
			change = true;
			for (VTodoChanges.ItemDelete item : changes.delete) {
				try {
					doDelete(item.uid);
					ret.removed.add(item.uid);
				} catch (ServerFault sf) {
					if (sf.getCode() == ErrorCode.NOT_FOUND) {
						logger.warn("task uid {} was sent as deleted but does not exist.", item.uid);
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
		Item item = Item.create(null, id);
		return doUpdate(item, value).ack();
	}

	@Override
	public Ack createById(long id, VTodo value) {
		Item item = Item.create("todo-by-id:" + id, id);
		return create(item, value);
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
		ids.forEach(this::deleteById);
	}

	@Override
	public VTodo get(String uid) {
		ItemValue<VTodo> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<VTodo> todoItem, boolean isCreate) {
		if (isCreate) {
			create(todoItem.item(), todoItem.value);
		} else {
			doUpdate(todoItem.item(), todoItem.value);
		}
	}

	@Override
	public List<ItemValue<VTodo>> getByIcsUid(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());
		return storeService.getByIcsUid(uid);
	}

}

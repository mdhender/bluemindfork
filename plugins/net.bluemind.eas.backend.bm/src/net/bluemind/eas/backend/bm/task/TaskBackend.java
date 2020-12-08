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
package net.bluemind.eas.backend.bm.task;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSTask;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyType;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.tasks.TasksResponse;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

public class TaskBackend extends CoreConnect {

	private TaskConverter converter;
	private final ISyncStorage storage;

	public TaskBackend(ISyncStorage storage) {
		converter = new TaskConverter();
		this.storage = storage;
	}

	public Changes getContentChanges(BackendSession bs, long version, CollectionId collectionId)
			throws ActiveSyncException {
		Changes changes = new Changes();

		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
			ITodoList service = getService(bs, folder.containerUid);

			ContainerChangeset<Long> changeset = service.changesetById(version);
			logger.debug("[{}][{}] get task changes. created: {}, updated: {}, deleted: {}, folder: {}, version: {}",
					bs.getLoginAtDomain(), bs.getDevId(), changeset.created.size(), changeset.updated.size(),
					changeset.deleted.size(), folder.containerUid, version);

			changes.version = changeset.version;

			for (long id : changeset.created) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.TASKS, ChangeType.ADD));
			}

			for (long id : changeset.updated) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.TASKS, ChangeType.CHANGE));
			}

			for (long id : changeset.deleted) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.TASKS, ChangeType.DELETE));
			}

		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
				logger.warn(e.getMessage());
			} else {
				logger.error(e.getMessage(), e);
			}
			changes.version = version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// BM-7227
			// Something went wrong
			// Send current version number to prevent full sync
			changes.version = version;
		}

		return changes;
	}

	public CollectionItem store(BackendSession bs, CollectionId collectionId, Optional<String> sid,
			IApplicationData data, ConflicResolution conflictPolicy, SyncState syncState) throws ActiveSyncException {
		CollectionItem ret = null;
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ITodoList service = getService(bs, folder.containerUid);
		try {
			if (sid.isPresent()) {
				String serverId = sid.get();
				Long id = getItemId(serverId);

				if (id != null) {
					ItemValue<VTodo> item = service.getCompleteById(id);
					if (item == null) {
						logger.debug("Fail to find VTodo {}", id);
						return CollectionItem.of(collectionId, id);
					}

					if (conflictPolicy == ConflicResolution.SERVER_WINS && item.version > syncState.version) {
						throw new ActiveSyncException(
								"Both server and client changes. Conflict resolution is SERVER_WINS");
					}

					VTodo oldTodo = item.value;
					VTodo todo = converter.convert(data);

					if (todo.description == null) {
						// GLAG-72 desc can be ghosted
						todo.description = oldTodo.description;
					}

					// Do not loose location and categories
					todo.location = oldTodo.location;
					todo.categories = oldTodo.categories;

					try {
						service.updateById(id, todo);
						ret = CollectionItem.of(collectionId, id);
						logger.info("Update todo bs: {}, collection: {}, serverId: {}, summary: {}, completed: {}",
								bs.getLoginAtDomain(), folder.containerUid, serverId, todo.summary, todo.completed);
					} catch (Exception e) {
						logger.error("Fail to update todo bs:" + bs.getLoginAtDomain() + ", collection: "
								+ folder.containerUid + ", serverId: " + serverId + ", summary:" + todo.summary);
					}
				}

			} else {
				VTodo event = converter.convert(data);
				String uid = UUID.randomUUID().toString();
				service.create(uid, event);

				ItemValue<VTodo> created = service.getComplete(uid);
				ret = CollectionItem.of(collectionId, created.internalId);
			}

		} catch (ServerFault e) {
			throw new ActiveSyncException(e);
		}

		return ret;
	}

	private ITodoList getService(BackendSession bs, String containerUid) throws ServerFault {
		return getTodoListService(bs, containerUid);
	}

	public void delete(BackendSession bs, Collection<CollectionItem> serverIds) throws ActiveSyncException {
		if (serverIds != null) {
			try {
				for (CollectionItem serverId : serverIds) {
					HierarchyNode folder = storage.getHierarchyNode(bs, serverId.collectionId);
					ITodoList service = getService(bs, folder.containerUid);

					service.deleteById(serverId.itemId);
				}
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					throw new ActiveSyncException(e);
				}
				logger.error(e.getMessage(), e);
			}
		}

	}

	public AppData fetch(BackendSession bs, ItemChangeReference ic) throws ActiveSyncException {
		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, ic.getServerId().collectionId);
			ITodoList service = getService(bs, folder.containerUid);

			ItemValue<VTodo> todo = service.getCompleteById(ic.getServerId().itemId);
			AppData ret = toAppData(bs, todo);

			return ret;
		} catch (Exception e) {
			throw new ActiveSyncException(e.getMessage(), e);
		}
	}

	public Map<Long, AppData> fetchMultiple(BackendSession bs, CollectionId collectionId, List<Long> ids)
			throws ActiveSyncException {
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ITodoList service = getService(bs, folder.containerUid);

		List<ItemValue<VTodo>> todos = service.multipleGetById(ids);
		Map<Long, AppData> res = new HashMap<>(ids.size());
		todos.stream().forEach(todo -> {
			try {
				AppData data = toAppData(bs, todo);
				res.put(todo.internalId, data);
			} catch (Exception e) {
				logger.error("Fail to convert todo {}", todo.uid, e);
			}
		});

		return res;
	}

	private AppData toAppData(BackendSession bs, ItemValue<VTodo> todo) {
		MSTask msTask = converter.convert(todo.value, bs.getUser().getTimeZone());

		TasksResponse cr = OldFormats.update(msTask);
		AppData data = AppData.of(cr);

		if (msTask.description != null && !msTask.description.trim().isEmpty()) {
			final AirSyncBaseResponse airSyncBase = new AirSyncBaseResponse();
			airSyncBase.body = new AirSyncBaseResponse.Body();
			airSyncBase.body.type = BodyType.PlainText;
			airSyncBase.body.data = DisposableByteSource.wrap(msTask.description.trim());
			airSyncBase.body.estimatedDataSize = (int) airSyncBase.body.data.size();
			data.body = LazyLoaded.loaded(airSyncBase);
		}
		return data;
	}

}

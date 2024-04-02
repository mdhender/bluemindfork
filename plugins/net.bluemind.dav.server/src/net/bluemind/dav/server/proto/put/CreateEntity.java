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
package net.bluemind.dav.server.proto.put;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.adapter.AddressbookOwner;
import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper.CalendarProperties;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.ics.ICS;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;

abstract class CreateEntity {

	protected Logger logger = LoggerFactory.getLogger(CreateEntity.class);

	public abstract void create(LoggedCore lc, PutQuery query, PutResponse pr, ContainerDescriptor cal)
			throws ServerFault;

	public static CreateEntity getByType(String type) {
		if (null != type) {
			if ("calendar".equals(type)) {
				return new CreateCalendar();
			} else if ("todolist".equals(type)) {
				return new CreateTodoList();
			} else if ("addressbook".equals(type)) {
				return new CreateAddressbook();
			}
		}
		return new NoOp();
	}

	static class CreateTodoList extends CreateEntity {

		public void create(LoggedCore lc, PutQuery query, PutResponse pr, ContainerDescriptor cal) throws ServerFault {
			new VTodoAdapter();

			List<ItemValue<VTodo>> todos = new VTodoAdapter().convertToVTodoList(((CalendarPutQuery) query).calendar);

			if (todos.size() != 1) {
				logger.error("we only support one task put", new Exception());
				pr.setStatus(500);
				return;
			}

			logger.info("todolist uid is {}", cal.uid);
			ItemValue<VTodo> todo = todos.get(0);
			if (query.isCreate()) {
				createTask(lc, cal.uid, todo, query, pr);
			} else {
				updateTask(lc, cal.uid, todo, query, pr);
			}
		}

		private void updateTask(LoggedCore lc, String todolistUid, ItemValue<VTodo> todo, PutQuery query,
				PutResponse pr) {

			logger.info("[{}] update task uid {} : {}", todolistUid, todo.uid, todo.value);
			try {
				ITodoList todolist = lc.getCore().instance(ITodoList.class, todolistUid);
				todolist.update(todo.uid, todo.value);
				ItemValue<VTodo> lastOne = todolist.getComplete(todo.uid);
				pr.setEtag(SyncTokens.getEtag(query.getPath(), lastOne.version));
				pr.setStatus(200);
			} catch (ServerFault e) {
				logger.error("error during task update", e);
				if (e.getCode() == ErrorCode.NOT_FOUND) {
					logger.warn("task {}/{} not found, create it", todolistUid, todo.uid);
					createTask(lc, todolistUid, todo, query, pr);
				} else {
					pr.setStatus(500);
				}
			} catch (Exception e) {
				pr.setStatus(500);
				logger.error("error during task creation", e);
			}
		}

		private void createTask(LoggedCore lc, String todolistUid, ItemValue<VTodo> todo, PutQuery query,
				PutResponse pr) {

			logger.info("[{}] create task uid {}", todolistUid, todo.uid);
			try {
				ITodoList todolist = lc.getCore().instance(ITodoList.class, todolistUid);

				todolist.create(todo.uid, todo.value);
				ItemValue<VTodo> lastOne = todolist.getComplete(todo.uid);
				pr.setEtag(SyncTokens.getEtag(query.getPath(), lastOne.version));
				pr.setStatus(201);
			} catch (Exception e) {
				pr.setStatus(500);
				logger.error("error during task creation", e);
			}
		}
	}

	static class CreateCalendar extends CreateEntity {

		@Override
		public void create(LoggedCore lc, PutQuery query, PutResponse pr, ContainerDescriptor cal) throws ServerFault {
			String itemUid = query.getExtId();
			logger.info("[{}] VEvent uid from query is {}", cal.uid, itemUid);
			List<ItemValue<VEventSeries>> events = new LinkedList<>();
			Consumer<ItemValue<VEventSeries>> consumer = series -> events.add(series);
			CalendarProperties calendarProperties = VEventServiceHelper.parseCalendar(
					new ByteArrayInputStream(((CalendarPutQuery) query).calendar.getBytes()), Optional.empty(),
					Collections.emptyList(), consumer);

			ICalendar calApi = lc.getCore().instance(ICalendar.class, cal.uid);
			if (events.size() != 1) {
				throw new ServerFault(events.size() + " events have been submitted for creation");
			}

			ItemValue<VEventSeries> series = events.get(0);

			ICS.adaptClassification(calendarProperties, Arrays.asList(series));

			ItemValue<VEventSeries> current = calApi.getComplete(itemUid);
			ContainerUpdatesResult res = null;
			if (current == null) {
				VEventChanges changes = new VEventChanges();
				changes.add = Arrays.asList(VEventChanges.ItemAdd.create(itemUid, series.value, true));
				res = calApi.updates(changes);

			} else {
				VEventChanges changes = new VEventChanges();
				changes.modify = Arrays.asList(VEventChanges.ItemModify.create(itemUid, series.value, true));
				res = calApi.updates(changes);
			}
			pr.setEtag(SyncTokens.getEtag(query.getPath(), res.version));
			pr.setStatus(query.isCreate() ? 201 : 200);
		}

	}

	static class CreateAddressbook extends CreateEntity {

		@Override
		public void create(LoggedCore lc, PutQuery query, PutResponse pr, ContainerDescriptor ab) throws ServerFault {
			String itemUid = query.getExtId();
			logger.info("[{}] Contact uid from query is {}", ab.uid, itemUid);

			AddressbookPutQuery abQuery = (AddressbookPutQuery) query;
			List<net.fortuna.ical4j.vcard.VCard> parsed = VCardAdapter.parse(abQuery.addressbook);
			if (parsed.size() != 1) {
				throw new ServerFault(parsed.size() + " vcards have been submitted for creation");
			}

			String seed = "" + System.currentTimeMillis();
			ItemValue<VCard> adaptedCard = VCardAdapter.adaptCard(parsed.get(0),
					s -> UUID.nameUUIDFromBytes(seed.concat(s).getBytes()).toString(),
					Optional.of(new AddressbookOwner(lc.getDomain(), lc.getUser().uid, Kind.USER)),
					Collections.emptyList());

			IAddressBook abApi = lc.getCore().instance(IAddressBook.class, ab.uid);

			ItemValue<VCard> current = abApi.getComplete(itemUid);
			VCardChanges changes = new VCardChanges();
			if (current == null) {
				changes.add = Arrays.asList(VCardChanges.ItemAdd.create(itemUid, adaptedCard.value));
			} else {
				changes.modify = Arrays.asList(VCardChanges.ItemModify.create(itemUid, adaptedCard.value));
			}
			ContainerUpdatesResult res = abApi.updates(changes);

			pr.setEtag(SyncTokens.getEtag(query.getPath(), res.version));
			pr.setStatus(query.isCreate() ? 201 : 200);
		}

	}

	static class NoOp extends CreateEntity {

		@Override
		public void create(LoggedCore lc, PutQuery query, PutResponse pr, ContainerDescriptor cal) throws ServerFault {
		}

	}

}

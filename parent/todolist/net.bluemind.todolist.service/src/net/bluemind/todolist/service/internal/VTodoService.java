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

import java.util.Iterator;
import java.util.List;

import io.vertx.core.buffer.Buffer;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.todolist.adapter.VTodoAdapter;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.IVTodo;
import net.bluemind.todolist.api.VTodo;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.util.Strings;

public class VTodoService implements IVTodo {

	private ITodoList todoService;
	private BmContext context;

	// ACL checked in todolist service

	public VTodoService(BmContext context, ITodoList service) {
		this.context = context;
		todoService = service;
	}

	@Override
	public Stream exportAll() throws ServerFault {

		List<ItemValue<VTodo>> todos = todoService.all();
		return export(todos);
	}

	@Override
	public Stream exportTodos(List<String> uids) throws ServerFault {
		List<ItemValue<VTodo>> todos = todoService.multipleGet(uids);
		return export(todos);
	}

	private Stream export(List<ItemValue<VTodo>> todos) {
		final Iterator<ItemValue<VTodo>> iterator = todos.iterator();

		GenericObjectStream<String> stream = new GenericObjectStream<String>() {

			private final int NOT_INITIALIZED = 0;
			private final int IN_PROGRESS = 1;
			private final int CLOSED = 3;

			private int state = NOT_INITIALIZED;

			@Override
			protected Buffer serialize(String n) throws Exception {
				return Buffer.buffer(n);
			}

			@Override
			protected String next() throws Exception {
				final StringBuffer buffer = new StringBuffer();
				switch (state) {
				case NOT_INITIALIZED:
					state = IN_PROGRESS;
					Calendar todolist = VTodoAdapter.createTodoList();
					buffer.append(Calendar.BEGIN);
					buffer.append(':');
					buffer.append(Calendar.VCALENDAR);
					buffer.append(Strings.LINE_SEPARATOR);
					buffer.append(todolist.getProperties());
					buffer.append(Strings.LINE_SEPARATOR);
					return buffer.toString();
				case IN_PROGRESS:
					if (iterator.hasNext()) {
						ItemValue<VTodo> vtodo = iterator.next();
						return VTodoAdapter.adaptTodo(vtodo.uid, vtodo.value).toString();
					} else {
						state = CLOSED;
						buffer.append(Calendar.END);
						buffer.append(':');
						buffer.append(Calendar.VCALENDAR);
						return buffer.toString();
					}
				case CLOSED:
				default:
					return null;
				}
			}
		};

		return VertxStream.stream(stream);
	}

	@Override
	public TaskRef importIcs(String ics) throws ServerFault {
		return context.provider().instance(ITasksManager.class).run(new VTodoImportTask(todoService, ics));
	}

}

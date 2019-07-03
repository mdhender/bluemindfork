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

package net.bluemind.todolist.api;

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class TodoListsVTodoQuery {
	public String owner;
	public List<String> containers;
	public VTodoQuery vtodoQuery;

	public static TodoListsVTodoQuery create(VTodoQuery vtodoQuery, List<String> containers) {
		TodoListsVTodoQuery ret = new TodoListsVTodoQuery();
		ret.containers = containers;
		ret.vtodoQuery = vtodoQuery;
		return ret;
	}

	public static TodoListsVTodoQuery create(VTodoQuery vtodoQuery, String owner) {
		TodoListsVTodoQuery ret = new TodoListsVTodoQuery();
		ret.owner = owner;
		ret.vtodoQuery = vtodoQuery;
		return ret;
	}

}

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

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.date.BmDateTime;

@BMApi(version = "3")
public class VTodoQuery {

	public int from = 0;
	public int size = -1;
	public String query;
	public boolean escapeQuery;
	public String todoUid;

	/**
	 * Lower bound of the search range
	 */
	public BmDateTime dateMin;

	/**
	 * Higher bound of the search range
	 */
	public BmDateTime dateMax;

	/**
	 * @param query
	 * @return
	 */
	public static VTodoQuery create(String query) {
		VTodoQuery q = new VTodoQuery();
		q.size = -1;
		q.from = 0;
		q.query = query;
		q.escapeQuery = false;
		return q;
	}

	/**
	 * @param dateMin
	 * @param dateMax
	 * @return
	 */
	public static VTodoQuery create(BmDateTime dateMin, BmDateTime dateMax) {
		VTodoQuery q = new VTodoQuery();
		q.from = 0;
		q.size = -1;
		q.dateMin = dateMin;
		q.dateMax = dateMax;
		return q;
	}

}

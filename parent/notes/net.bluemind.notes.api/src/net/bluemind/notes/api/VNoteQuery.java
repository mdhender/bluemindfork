package net.bluemind.notes.api;
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class VNoteQuery {

	public int from = 0;
	public int size = -1;
	public String query;
	public boolean escapeQuery;

	/**
	 * @param query
	 * @return
	 */
	public static VNoteQuery create(String query) {
		VNoteQuery q = new VNoteQuery();
		q.size = -1;
		q.from = 0;
		q.query = query;
		q.escapeQuery = false;
		return q;
	}
}

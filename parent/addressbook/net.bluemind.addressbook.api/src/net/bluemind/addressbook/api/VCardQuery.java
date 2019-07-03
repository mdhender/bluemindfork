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
package net.bluemind.addressbook.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class VCardQuery {

	@BMApi(version = "3")
	public static enum OrderBy {
		FormatedName, Pertinance
	}

	public int from = 0;
	public int size = -1;
	public String query;
	public OrderBy orderBy = OrderBy.FormatedName;
	public boolean escapeQuery;

	public static VCardQuery create(String query) {
		VCardQuery q = new VCardQuery();
		q.size = -1;
		q.from = 0;
		q.query = query;
		q.escapeQuery = false;
		return q;
	}
}

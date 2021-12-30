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

package net.bluemind.notes.api;

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class VNotesQuery {
	public String owner;
	public List<String> containers;
	public VNoteQuery vnoteQuery;

	public static VNotesQuery create(VNoteQuery vnoteQuery, List<String> containers) {
		VNotesQuery ret = new VNotesQuery();
		ret.containers = containers;
		ret.vnoteQuery = vnoteQuery;
		return ret;
	}

	public static VNotesQuery create(VNoteQuery vnoteQuery, String owner) {
		VNotesQuery ret = new VNotesQuery();
		ret.owner = owner;
		ret.vnoteQuery = vnoteQuery;
		return ret;
	}

}

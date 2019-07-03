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
package net.bluemind.ui.adminconsole.system.domains.assignments;

import com.google.gwt.json.client.JSONArray;

import net.bluemind.server.api.gwt.js.JsAssignment;

public class AssignmentAction {
	public final String serverUid;
	public final String tag;
	public final Action action;

	public AssignmentAction(String serverUid, String tag, Action action) {
		this.serverUid = serverUid;
		this.tag = tag;
		this.action = action;
	}

	public static enum Action {
		assign, unassign;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((serverUid == null) ? 0 : serverUid.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AssignmentAction other = (AssignmentAction) obj;
		if (action != other.action)
			return false;
		if (serverUid == null) {
			if (other.serverUid != null)
				return false;
		} else if (!serverUid.equals(other.serverUid))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}

	public static boolean isAssigned(JsAssignment assignment, JSONArray list) {
		for (int i = 0; i < list.size(); i++) {
			JsAssignment a = list.get(i).isObject().getJavaScriptObject().cast();
			if (a.getServerUid().equals(assignment.getServerUid())
					&& a.getTag().equals(assignment.getTag())) {
				return true;
			}
		}
		return false;
	}

}

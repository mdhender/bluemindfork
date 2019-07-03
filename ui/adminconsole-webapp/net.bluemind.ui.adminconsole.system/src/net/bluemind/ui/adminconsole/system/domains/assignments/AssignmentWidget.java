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

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.server.api.gwt.js.JsAssignment;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;

public abstract class AssignmentWidget extends CompositeGwtWidgetElement {

	private static final String defaultEntryText = "---";

	protected abstract List<TagListBoxMapping> getMapping();

	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JSONArray domainAssignments = new JSONArray(map.get(DomainKeys.domainAssignments.name()));
		JsArray<JsItemValue<JsServer>> allServers = map.get(DomainKeys.allServers.name()).cast();
		for (int i = 0; i < allServers.length(); i++) {
			JsItemValue<JsServer> serverObject = allServers.get(i);
			for (TagListBoxMapping mapping : getMapping()) {
				checkDefaultEntry(mapping.listbox);
				fillListBox(domainAssignments, serverObject, mapping.tag, mapping.listbox);
			}
		}
		map.put(DomainKeys.currentDomainAssignments.name(), new JSONArray().getJavaScriptObject());
	}

	private void checkDefaultEntry(ListBox listbox) {
		if (listbox.getItemCount() == 0) {
			listbox.addItem(defaultEntryText);
		}
	}

	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JSONArray newAssignments = new JSONArray();
		String domainUid = map.getString(DomainKeys.domainUid.name());
		for (TagListBoxMapping mapping : getMapping()) {
			getSelectedAssignments(newAssignments, domainUid, mapping.listbox, mapping.tag);
		}
		JavaScriptObject assignmentMapObject = map.get(DomainKeys.currentDomainAssignments.name());
		if (null != assignmentMapObject) {
			newAssignments = mergeArrays(newAssignments, new JSONArray(assignmentMapObject));
		}
		map.put(DomainKeys.currentDomainAssignments.name(), newAssignments.getJavaScriptObject());
	}

	protected void fillListBox(JSONArray domainAssignments, JsItemValue<JsServer> serverObject, String tag,
			ListBox list) {
		if (serverIsTagged(serverObject, tag)) {
			list.addItem(serverObject.getValue().getName(), serverObject.getUid());
			selectIfAssigned(domainAssignments, tag, serverObject.getUid(), list);
		}
	}

	protected void selectIfAssigned(JSONArray domainAssignments, String tag, String serverUid, ListBox listbox) {
		for (int i = 0; i < domainAssignments.size(); i++) {
			JsAssignment assignment = domainAssignments.get(i).isObject().getJavaScriptObject().cast();
			if (assignment.getServerUid().equals(serverUid) && assignment.getTag().equals(tag)) {
				listbox.setItemSelected(listbox.getItemCount() - 1, true);
				return;
			}
		}
	}

	protected boolean serverIsTagged(JsItemValue<JsServer> serverObject, String tag) {
		JsArrayString tags = serverObject.getValue().getTags();
		for (int i = 0; i < tags.length(); i++) {
			String serverTag = tags.get(i);
			if (serverTag.equals(tag)) {
				return true;
			}
		}
		return false;
	}

	protected void getSelectedAssignments(JSONArray newAssignments, String domainUid, ListBox list, String tag) {
		for (int i = 0; i < list.getItemCount(); i++) {
			if (list.isItemSelected(i) && !list.getItemText(i).equals(defaultEntryText)) {
				String serverUid = list.getValue(i);
				JsAssignment assignment = JsAssignment.create();
				assignment.setDomainUid(domainUid);
				assignment.setServerUid(serverUid);
				assignment.setTag(tag);
				newAssignments.set(newAssignments.size(), new JSONObject(assignment));
			}
		}
	}

	private static JSONArray mergeArrays(JSONArray dest, JSONArray source) {
		for (int i = 0; i < source.size(); i++) {
			dest.set(dest.size(), source.get(i));
		}
		return dest;
	}

	public static class TagListBoxMapping {
		public final String tag;
		public final ListBox listbox;

		public TagListBoxMapping(String tag, ListBox listBox) {
			this.tag = tag;
			this.listbox = listBox;
		}

	}

}

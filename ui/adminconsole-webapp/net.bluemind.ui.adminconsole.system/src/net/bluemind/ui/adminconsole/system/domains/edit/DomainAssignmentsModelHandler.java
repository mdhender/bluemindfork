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
package net.bluemind.ui.adminconsole.system.domains.edit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.server.api.gwt.js.JsAssignment;
import net.bluemind.server.api.gwt.serder.AssignmentGwtSerDer;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.adminconsole.system.domains.assignments.AssignmentAction;
import net.bluemind.ui.adminconsole.system.domains.assignments.AssignmentAction.Action;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainAssignmentsModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.DomainAssignmentsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainAssignmentsModelHandler();
			}
		});
		GWT.log("bm.ac.DomainAssignmentsModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		final ServerGwtEndpoint serverService = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		loadAssignments(handler, map, domainUid, serverService);
	}

	private void loadAssignments(final AsyncHandler<Void> handler, final JsMapStringJsObject map,
			final String domainUid, final ServerGwtEndpoint serverService) {
		serverService.getAssignments(domainUid, new DefaultAsyncHandler<List<Assignment>>(handler) {

			@Override
			public void success(List<Assignment> assignments) {
				JSONArray domainAssignments = new JSONArray();
				for (int i = 0; i < assignments.size(); i++) {
					JsAssignment assignment = new AssignmentGwtSerDer().serialize(assignments.get(i)).isObject()
							.getJavaScriptObject().cast();
					domainAssignments.set(i, new JSONObject(assignment));
				}
				map.put(DomainKeys.domainAssignments.name(), domainAssignments.getJavaScriptObject());
				map.put(DomainKeys.currentDomainAssignments.name(), domainAssignments.getJavaScriptObject());
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		final ServerGwtEndpoint serverService = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");

		JSONArray assignments = new JSONArray(map.get(DomainKeys.currentDomainAssignments.name()));
		JSONArray oldAssignments = new JSONArray(map.get(DomainKeys.domainAssignments.name()));

		Set<AssignmentAction> assignmentActions = new HashSet<>();
		// detect new assignments
		for (int i = 0; i < assignments.size(); i++) {
			JsAssignment newAssignment = assignments.get(i).isObject().getJavaScriptObject().cast();
			if (!AssignmentAction.isAssigned(newAssignment, oldAssignments)) {
				assignmentActions.add(new AssignmentAction(newAssignment.getServerUid(), newAssignment.getTag(),
						AssignmentAction.Action.assign));
			}
		}
		// detect obsolete indexing assignments
		for (int i = 0; i < oldAssignments.size(); i++) {
			JsAssignment oldAssignment = oldAssignments.get(i).isObject().getJavaScriptObject().cast();
			if (!AssignmentAction.isAssigned(oldAssignment, assignments)) {
				assignmentActions.add(new AssignmentAction(oldAssignment.getServerUid(), oldAssignment.getTag(),
						AssignmentAction.Action.unassign));
			}
		}

		AssignmentAction[] actions = assignmentActions.toArray(new AssignmentAction[0]);

		executeAssignmentActions(actions, 0, serverService, handler, domainUid);

	}

	private void executeAssignmentActions(final AssignmentAction[] actions, final int index,
			final ServerGwtEndpoint serverService, final AsyncHandler<Void> handler, final String domainUid) {
		if (index == actions.length) {
			handler.success(null);
			return;
		}

		AssignmentAction action = actions[index];
		GWT.log("executing assignment action " + action.serverUid + " : " + action.tag + " : " + action.action.name());
		if (action.action == Action.assign) {
			serverService.assign(action.serverUid, domainUid, action.tag, new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
					executeAssignmentActions(actions, index + 1, serverService, handler, domainUid);
				}
			});
		} else {
			serverService.unassign(action.serverUid, domainUid, action.tag, new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
					executeAssignmentActions(actions, index + 1, serverService, handler, domainUid);
				}

			});
		}
	}

}

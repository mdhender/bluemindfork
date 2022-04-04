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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.directory.api.gwt.js.JsOrgUnitPath;
import net.bluemind.directory.api.gwt.serder.OrgUnitPathGwtSerDer;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.role.api.IRolesPromise;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.api.gwt.endpoint.RolesSockJsEndpoint;
import net.bluemind.ui.adminconsole.base.orgunit.OrgUnitAdministratorModel;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtrole.client.RolesEditor;
import net.bluemind.ui.gwtrole.client.RolesModel;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class OrgUnitsAdminRolesTree extends Composite {

	private static final String FAKE_ROOT_UID = "ROOT";
	public static final String TYPE = "bm.role.OrgUnitsRolesEditor";

	ScrollPanel rolesPanel;

	private Set<RolesCategory> rolesCategories;

	private Set<RoleDescriptor> roles;

	private Map<String, OrgUnitAdministratorModel> ouRoles = new HashMap<>();

	private JsArrayString inheritedRoles;

	public OrgUnitsAdminRolesTree() {
		rolesPanel = new ScrollPanel();
		rolesPanel.setHeight("350px");
		initWidget(rolesPanel);
	}

	private void loadModelForOrgUnit(String selectedValue) {
		RolesEditor ouRolesEditor = new RolesEditor();
		RolesModel model = JavaScriptObject.createObject().cast();
		model.setNativeCategories(rolesCategories);
		model.setReadOnly(true);
		OrgUnitAdministratorModel orgUnitAdministratorModel = ouRoles.get(selectedValue);
		model.setNativeRoles(roles);
		Set<String> parentRoles = new HashSet<>(parentRoles(orgUnitAdministratorModel.orgUnit.getParent()));
		parentRoles.addAll(Arrays.asList(ouRoles.get(FAKE_ROOT_UID).roles));
		model.setInheritedRoles(asJsStringArray(parentRoles));
		model.setRoles(asJsStringArray(orgUnitAdministratorModel.roles));
		ouRolesEditor.loadModel(model);
		rolesPanel.add(ouRolesEditor);
	}

	private Set<String> parentRoles(JsOrgUnitPath orgUnit) {
		return parentRoles(orgUnit, oo -> {
			OrgUnitAdministratorModel ouModel = ouRoles.get(oo.getUid());
			if (ouModel != null) {
				return new HashSet<>(Arrays.asList(ouModel.roles));
			} else {
				return Collections.emptySet();
			}
		});
	}

	private Set<String> parentRoles(JsOrgUnitPath orgUnit, Function<JsOrgUnitPath, Set<String>> func) {
		if (orgUnit == null) {
			return Collections.emptySet();
		}

		Set<String> parentRoles = new HashSet<>();
		parentRoles.addAll(func.apply(orgUnit));
		parentRoles.addAll(parentRoles(orgUnit.getParent(), func));
		return parentRoles;
	}

	public void loadOuRoleTreeContext(String entryUid, String domainUid, OrgUnitItem orgUnitItem) {
		clearRoles();
		loadRolesModel();

		IOrgUnitsPromise orgUnits = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		orgUnits.listByAdministrator(entryUid, Collections.emptyList()).thenAccept(res ->

		res.stream().filter(o -> o.uid.equals(orgUnitItem.getUid())).findFirst().ifPresent(unit -> {
			List<CompletableFuture<OrgUnitAdministratorModel>> adminRolesCF = getAdminRolesCF(orgUnits, entryUid, unit);

			CompletableFuture.allOf(adminRolesCF.toArray(new CompletableFuture[0])) //
					.thenApply(v -> adminRolesCF.stream().map(CompletableFuture::join).collect(Collectors.toList())) //
					.thenAccept(l -> {
						IUserPromise userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
								.promiseApi();
						userService.getRoles(entryUid) //
								.thenAccept(userRoles -> {
									OrgUnitAdministratorModel rootModel = GWT.create(OrgUnitAdministratorModel.class);
									JsOrgUnitPath rootOrgUnit = JavaScriptObject.createObject().cast();
									rootOrgUnit.setUid(FAKE_ROOT_UID);
									rootModel.orgUnit = rootOrgUnit;
									rootModel.roles = userRoles.toArray(new String[0]);
									addOrgUnit(rootModel);
								}) //
								.thenRun(() -> loadModelForOrgUnit(orgUnitItem.getUid()));
					});
		}));
	}

	private List<CompletableFuture<OrgUnitAdministratorModel>> getAdminRolesCF(IOrgUnitsPromise orgUnits,
			String entryUid, OrgUnitPath o) {
		List<JsOrgUnitPath> orgUnitsPath = loadParentHierarchie(o);

		return orgUnitsPath.stream()
				.map(p -> orgUnits.getAdministratorRoles(p.getUid(), entryUid, Collections.emptyList()).thenApply(r -> {
					OrgUnitAdministratorModel ouModelForCurrentUnit = createOuModel(p);
					ouModelForCurrentUnit.roles = r.toArray(new String[0]);
					addOrgUnit(ouModelForCurrentUnit);
					return ouModelForCurrentUnit;
				}).exceptionally(e -> {
					Notification.get().reportError(e);
					return null;
				})).collect(Collectors.toList());
	}

	private static OrgUnitAdministratorModel createOuModel(JsOrgUnitPath p) {
		OrgUnitAdministratorModel ouModel = GWT.create(OrgUnitAdministratorModel.class);
		ouModel.modified = false;
		ouModel.roles = new String[0];
		ouModel.orgUnit = p;
		return ouModel;
	}

	private static List<JsOrgUnitPath> loadParentHierarchie(OrgUnitPath o) {
		List<JsOrgUnitPath> list = new ArrayList<>();
		getParent(list, o);
		return list;
	}

	private static void getParent(List<JsOrgUnitPath> list, OrgUnitPath o) {
		list.add(new OrgUnitPathGwtSerDer().serialize(o).isObject().getJavaScriptObject().cast());
		if (o.parent != null) {
			getParent(list, o.parent);
		}
	}

	private void loadRolesModel() {
		final IRolesPromise ep = new RolesSockJsEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		if (inheritedRoles == null) {
			inheritedRoles = JsArrayString.createArray().cast();
		}
		CompletableFuture.allOf(
				// load roles and categories
				ep.getRolesCategories().thenAccept(value -> rolesCategories = value),
				// load roles
				ep.getRoles().thenAccept(
						value -> roles = value.stream().filter(v -> v.dirEntryPromote).collect(Collectors.toSet()))) //
				.exceptionally(t -> {
					Notification.get().reportError(t);
					return null;
				});
	}

	public void clearRoles() {
		rolesPanel.clear();
		ouRoles.clear();
	}

	private void addOrgUnit(OrgUnitAdministratorModel o) {
		if (ouRoles.containsKey(o.orgUnit.getUid()) && !ouRoles.get(o.orgUnit.getUid()).deleted) {
			return;
		}
		ouRoles.put(o.orgUnit.getUid(), o);
	}

	private JsArrayString asJsStringArray(String[] roles2) {
		JsArrayString ret = JavaScriptObject.createArray().cast();
		for (int i = 0; i < roles2.length; i++) {
			ret.push(roles2[i]);
		}
		return ret;
	}

	private JsArrayString asJsStringArray(Collection<String> roles) {
		JsArrayString ret = JavaScriptObject.createArray().cast();
		for (String role : roles) {
			ret.push(role);
		}
		return ret;
	}
}
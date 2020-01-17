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
package net.bluemind.ui.adminconsole.base.orgunit;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.directory.api.gwt.js.JsOrgUnitPath;
import net.bluemind.directory.api.gwt.serder.OrgUnitPathGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.role.api.IRolesPromise;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.api.gwt.endpoint.RolesSockJsEndpoint;
import net.bluemind.ui.adminconsole.base.orgunit.l10n.OrgUnitConstants;
import net.bluemind.ui.common.client.OverlayScreen;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtrole.client.RolesEditor;
import net.bluemind.ui.gwtrole.client.RolesModel;

public class OrgUnitsAdministratorEditor extends CompositeGwtWidgetElement {

	interface OrgUnitsAdministratorEditorUiBinder extends UiBinder<HTMLPanel, OrgUnitsAdministratorEditor> {

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new OrgUnitsAdministratorEditor();
			}
		});
	}

	private static final String FAKE_ROOT_UID = "ROOT";
	public static final String TYPE = "bm.role.OrgUnitsAdministrator";

	@UiField
	ListBox orgUnits;

	@UiField
	FlowPanel rolesPanel;

	private OrgUnitsAdministratorEditorUiBinder binder = GWT.create(OrgUnitsAdministratorEditorUiBinder.class);

	private String domainUid;

	private Set<RolesCategory> rolesCategories;

	private Set<RoleDescriptor> roles;

	private Map<String, OrgUnitAdministratorModel> ouRoles = new HashMap<>();

	private RolesEditor selectedUnit;
	private String selectedOrgUnit = null;
	private boolean readOnly;

	@UiField
	HTML orgUnitsLabel;

	@UiField
	Button trash;

	@UiHandler("trash")
	public void trash(ClickEvent e) {
		removeOrgUnit();
	}

	private Set<RoleDescriptor> allRoles;

	private JsArrayString inheritedRoles;

	public OrgUnitsAdministratorEditor() {
		initWidget(binder.createAndBindUi(this));
		orgUnits.addChangeHandler(h -> {
			selectedOrgUnit(orgUnits.getSelectedValue());
		});
	}

	private void selectedOrgUnit(String selectedValue) {
		if (selectedOrgUnit != null) {
			saveSelectedOrgUnit();
			rolesPanel.clear();
			selectedOrgUnit = null;
		}

		if (selectedValue != null) {
			if (!selectedValue.equals(FAKE_ROOT_UID)) {
				IDirectoryPromise dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
				dir.getRolesForOrgUnit(selectedValue).thenAccept(roles -> {
					loadModelForOrgUnit(selectedValue, this.allRoles.stream().filter(new SearchReadOnlyRoles(roles))
							.map(r -> r.id).collect(Collectors.toSet()));
				});

			} else {
				// load roles for domain
				IDirectoryPromise dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
				dir.getRolesForDirEntry(domainUid).thenAccept(roles -> {
					loadModelForOrgUnit(selectedValue, this.allRoles.stream().filter(new SearchReadOnlyRoles(roles))
							.map(r -> r.id).collect(Collectors.toSet()));
				});

			}
		}

	}

	private static class SearchReadOnlyRoles implements Predicate<RoleDescriptor> {

		private final Set<String> roles;

		SearchReadOnlyRoles(Set<String> roles) {
			this.roles = roles;
		}

		@Override
		public boolean test(RoleDescriptor r) {
			if (r.selfPromote && includeRole(r)) {
				return false;
			}
			return !(roles.contains(r.id) || r.delegable);
		}

		private boolean includeRole(RoleDescriptor roleDesc) {
			boolean checkChildren = roleDesc.childsRole != null && roleDesc.childsRole.stream().anyMatch(roles::contains);
			boolean checkParent = roleDesc.parentRoleId != null && roles.contains(roleDesc.parentRoleId);
			return checkChildren || checkParent;
		}
	}

	private void loadModelForOrgUnit(String selectedValue, Set<String> readOnlyRoles) {

		RolesEditor ouRolesEditor = new RolesEditor();
		RolesModel model = JavaScriptObject.createObject().cast();
		model.setNativeCategories(rolesCategories);
		model.setReadOnly(readOnly);
		if (selectedValue.equals(FAKE_ROOT_UID)) {
			model.setNativeRoles(allRoles);
			model.setInheritedRoles(inheritedRoles);
			model.setReadOnlyRoles(asJsStringArray(readOnlyRoles));
			trash.setVisible(false);
		} else {
			model.setNativeRoles(roles);
			Set<String> roles = new HashSet<>(parentRoles(ouRoles.get(selectedValue).orgUnit.getParent()));
			roles.addAll(Arrays.asList(ouRoles.get(FAKE_ROOT_UID).roles));
			model.setInheritedRoles(asJsStringArray(roles));
			model.setReadOnlyRoles(asJsStringArray(readOnlyRoles));
			trash.setVisible(true);
		}
		model.setRoles(asJsStringArray(ouRoles.get(selectedValue).roles));
		ouRolesEditor.loadModel(model);
		rolesPanel.add(ouRolesEditor);
		selectedUnit = ouRolesEditor;
		selectedOrgUnit = selectedValue;
		ouRolesEditor.addValueChangeHandler(v -> {
			saveSelectedOrgUnit();
			refreshLabel();
		});
	}

	private Set<String> parentRoles(JsOrgUnitPath orgUnit) {
		return parentRoles(orgUnit, oo -> {
			OrgUnitAdministratorModel ouModel = ouRoles.get(orgUnit.getUid());
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

		Set<String> roles = new HashSet<>();
		roles.addAll(func.apply(orgUnit));
		roles.addAll(parentRoles(orgUnit.getParent(), func));
		return roles;
	}

	private void saveSelectedOrgUnit() {
		if (selectedOrgUnit != null) {
			OrgUnitAdministratorModel model = ouRoles.get(selectedOrgUnit);
			RolesModel rmodel = JavaScriptObject.createObject().cast();
			selectedUnit.saveModel(rmodel);
			model.roles = asStringArray(rmodel.getRoles());
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {

		JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");

		final IRolesPromise ep = new RolesSockJsEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		OrgUnitsAdministratorModel ousModel = OrgUnitsAdministratorModel.get(model);
		final RolesModel rolesModel = model.cast();
		readOnly = rolesModel.isReadOnly();
		inheritedRoles = rolesModel.getInheritedRoles();
		if (inheritedRoles == null) {
			inheritedRoles = JsArrayString.createArray().cast();
		}
		CompletableFuture.allOf(
				// load roles and categories
				ep.getRolesCategories().thenAccept(value -> rolesCategories = value),
				// load roles
				ep.getRoles().thenAccept(value -> {
					allRoles = value;
					roles = value.stream().filter(v -> v.dirEntryPromote).collect(Collectors.toSet());
				})).thenAccept(v -> {
					OrgUnitAdministratorModel rootModel = GWT.create(OrgUnitAdministratorModel.class);
					JsOrgUnitPath rootOrgUnit = JavaScriptObject.createObject().cast();
					rootOrgUnit.setUid(FAKE_ROOT_UID);
					rootOrgUnit.setName(OrgUnitConstants.INST.root());
					rootModel.orgUnit = rootOrgUnit;
					rootModel.roles = asStringArray(rolesModel.getRoles());
					addOrgUnit(rootModel);

					for (OrgUnitAdministratorModel o : ousModel.orgUnits) {
						addOrgUnit(o);
					}

					orgUnits.setSelectedIndex(0);
					selectedOrgUnit = null;
					selectedOrgUnit(FAKE_ROOT_UID);
					refreshLabel();

				}).exceptionally(t -> {
					return null;
				});
	}

	private void refreshLabel() {
		StringBuffer r = ouRoles.values().stream().map(ou -> {
			return descriptionOf(ou);
		}).reduce(new StringBuffer(), (b, v) -> {
			if (v != null) {
				b.append(v);
			}
			return b;
		}, (a, b) -> b);
		orgUnitsLabel.setHTML(r.toString());
	}

	private RoleDescriptor findRole(String role) {
		for (RoleDescriptor rd : allRoles) {
			if (rd.id.equals(role)) {
				return rd;
			}
		}
		return null;
	}

	private void removeOrgUnit() {

		// empty roles to remove delegation
		OrgUnitAdministratorModel model = ouRoles.get(selectedOrgUnit);
		RolesModel rmodel = JavaScriptObject.createObject().cast();
		selectedUnit.saveModel(rmodel);
		model.roles = new String[0];
		model.deleted = true;

		orgUnits.removeItem(orgUnits.getSelectedIndex());
		selectedOrgUnit = null;
		selectedOrgUnit(FAKE_ROOT_UID);
		refreshLabel();
	}

	private void addOrgUnit(OrgUnitAdministratorModel o) {
		if (ouRoles.containsKey(o.orgUnit.getUid()) && !ouRoles.get(o.orgUnit.getUid()).deleted) {
			return;
		}
		ouRoles.put(o.orgUnit.getUid(), o);
		orgUnits.addItem(OUUtils.toPath(o.orgUnit), o.orgUnit.getUid());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		saveSelectedOrgUnit();
		OrgUnitsAdministratorModel ousModel = OrgUnitsAdministratorModel.get(model);
		saveModel(ousModel);
		RolesModel rolesModel = model.cast();
		rolesModel.setRoles(asJsStringArray(ouRoles.get(FAKE_ROOT_UID).roles));
	}

	private void saveModel(OrgUnitsAdministratorModel ousModel) {
		ousModel.orgUnits = ouRoles.values().stream().filter(ou -> !ou.orgUnit.getUid().equals("ROOT")).map(ou -> {
			return ou;
		}).toArray(i -> new OrgUnitAdministratorModel[i]);
	}

	private String[] asStringArray(JsArrayString a) {
		String[] ret = new String[a.length()];
		for (int i = 0; i < a.length(); i++) {
			ret[i] = a.get(i);
		}
		return ret;
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

	@UiHandler("addDelegation")
	public void addDelegation(ClickEvent e) {
		AddOrgUnitRoles popup = new AddOrgUnitRoles(domainUid);

		popup.addDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				if (popup.getValue() != null) {
					OrgUnitAdministratorModel o = GWT.create(OrgUnitAdministratorModel.class);
					o.modified = true;
					o.roles = new String[0];
					o.orgUnit = new OrgUnitPathGwtSerDer().serialize(popup.getValue()).isObject().getJavaScriptObject()
							.cast();
					addOrgUnit(o);
					popup.hide();
				} else {
					// err msg?
				}
			}
		});

		SizeHint sh = popup.getSizeHint();
		final OverlayScreen os = new OverlayScreen(popup, sh.getWidth(), sh.getHeight());
		popup.setOverlay(os);
		os.center();
		popup.setFocus();
	}

	private String descriptionOf(OrgUnitAdministratorModel ou) {
		String path = "<b>" + OUUtils.toPath(ou.orgUnit) + "</b>";
		Stream<String> rolesStream = Arrays.stream(ou.roles);
		if (ou.orgUnit.getUid().equals("ROOT")) {
			List<String> ar = new ArrayList<>(Arrays.asList(ou.roles));
			ar.addAll(Arrays.asList(asStringArray(inheritedRoles)));
			rolesStream = ar.stream();
		} else {
			if (ou.roles.length == 0) {
				return null;
			}
		}
		String roles = rolesStream.map(role -> {
			RoleDescriptor rd = findRole(role);
			if (rd == null) {
				return null;
			}
			return "<b>\"" + rd.label + "\"</b>";
		}).filter(v -> v != null).collect(Collectors.joining(","));
		return "<div>" + OrgUnitConstants.INST.rolesOn(path, roles) + "</div>";
	}
}

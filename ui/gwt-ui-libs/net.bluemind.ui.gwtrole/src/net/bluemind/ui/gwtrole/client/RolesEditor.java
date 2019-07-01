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
package net.bluemind.ui.gwtrole.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.role.api.gwt.js.JsRoleDescriptor;
import net.bluemind.role.api.gwt.js.JsRolesCategory;
import net.bluemind.ui.gwtrole.client.i18n.RolesConstants;
import net.bluemind.ui.gwtrole.client.internal.UICategory;

public class RolesEditor extends CompositeGwtWidgetElement implements HasValueChangeHandlers<Void> {

	public static final String TYPE = "bm.role.RolesEditor";

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new RolesEditor();
			}
		});
	}

	private FlowPanel rootPanel;
	private Map<String, RolesCategoryPanel> categoriesMap;
	private boolean sysAdmin;
	private final List<ValueChangeHandler<Void>> valueChangeHandlers = new ArrayList<>();

	public RolesEditor() {
		this.rootPanel = new FlowPanel();
		initWidget(rootPanel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		RolesModel rolesModel = model.cast();
		JsArrayString inheritedRolesJS = rolesModel.getInheritedRoles();
		Set<String> inheritedRoles = new HashSet<>();
		if (inheritedRolesJS != null) {
			for (int i = 0; i < inheritedRolesJS.length(); i++) {
				inheritedRoles.add(inheritedRolesJS.get(i));
			}
		}

		JsArrayString readOnlyRolesJS = rolesModel.getReadOnlyRoles();
		Set<String> readOnlyRoles = new HashSet<>();
		if (readOnlyRolesJS != null) {
			for (int i = 0; i < readOnlyRolesJS.length(); i++) {
				readOnlyRoles.add(readOnlyRolesJS.get(i));
			}
		}

		boolean ro = rolesModel.isReadOnly();
		initRoles(inheritedRoles, readOnlyRoles, rolesModel, ro);
	}

	private void initRoles(Set<String> inheritedRoles, Set<String> readOnlyRoles, RolesModel rolesModel, boolean ro) {

		Label title = new Label(RolesConstants.INST.roles());
		title.setStyleName("sectionTitle");
		rootPanel.add(title);

		rootPanel.clear();

		JsArrayString roles = rolesModel.getRoles();
		Set<String> croles = new HashSet<>();
		for (int i = 0; i < roles.length(); i++) {
			String role = roles.get(i);
			croles.add(role);
		}

		// sort categories
		List<JsRolesCategory> categories = JsHelper.asList(rolesModel.getCategories());
		Collections.sort(categories, new Comparator<JsRolesCategory>() {

			@Override
			public int compare(JsRolesCategory o1, JsRolesCategory o2) {
				int i = o2.getPriority() - o1.getPriority();
				return i != 0 ? i : o1.getLabel().compareTo(o2.getLabel());
			}
		});

		categoriesMap = new HashMap<>();
		Map<String, UICategory> cMap = new HashMap<>();
		for (JsRolesCategory cat : categories) {
			UICategory uiCat = new UICategory();
			uiCat.category = cat;
			cMap.put(cat.getId(), uiCat);
		}

		List<JsRoleDescriptor> descriptors = JsHelper.asList(rolesModel.getDescciptors());

		// sort descriptors
		Collections.sort(descriptors, new Comparator<JsRoleDescriptor>() {

			@Override
			public int compare(JsRoleDescriptor o1, JsRoleDescriptor o2) {
				int i = o2.getPriority() - o1.getPriority();
				return i != 0 ? i : o1.getLabel().compareTo(o2.getLabel());
			}
		});

		while (!descriptors.isEmpty()) {
			for (Iterator<JsRoleDescriptor> it = descriptors.iterator(); it.hasNext();) {
				JsRoleDescriptor desc = it.next();
				UICategory cat = cMap.get(desc.getCategoryId());
				boolean force = true;
				if (desc.getParentRoleId() != null) {
					for (JsRoleDescriptor d : descriptors) {
						if (desc.getParentRoleId().equals(d.getId())) {
							force = false;
							break;
						}
					}
				}

				if (cat.addRole(desc, force)) {
					it.remove();
				}
			}
		}

		for (UICategory cat : cMap.values()) {
			if (!cat.roles.isEmpty()) {
				RolesCategoryPanel categoryPanel = new RolesCategoryPanel(cat);
				rootPanel.add(categoryPanel);
				categoryPanel.stRoles(croles, inheritedRoles, readOnlyRoles, ro);
				categoriesMap.put(cat.getId(), categoryPanel);

				categoryPanel.addValueChangeHandler(v -> {
					ValueChangeEvent.fire(RolesEditor.this, null);
				});
			}
		}

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		if (sysAdmin) {
			return;
		}
		RolesModel rolesModel = model.cast();

		JsArrayString roles = JsArrayString.createArray().cast();
		Set<String> r = new HashSet<>();
		for (RolesCategoryPanel panel : categoriesMap.values()) {
			r.addAll(panel.getRoles());
		}

		for (String role : r) {
			roles.push(role);
		}
		rolesModel.setRoles(roles);

	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler) {
		valueChangeHandlers.add(handler);
		return new HandlerRegistration() {

			@Override
			public void removeHandler() {
				valueChangeHandlers.remove(handler);
			}
		};
	}

	public void fireEvent(GwtEvent<?> event) {
		if (event instanceof ValueChangeEvent) {
			@SuppressWarnings("unchecked")
			ValueChangeEvent<Void> vce = (ValueChangeEvent<Void>) event;
			valueChangeHandlers.forEach(vch -> vch.onValueChange(vce));
		} else {
			super.fireEvent(event);
		}
	}

}

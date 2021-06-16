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
package net.bluemind.ui.adminconsole.directory.group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.ui.admin.client.forms.TextEdit;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.common.client.forms.StringEdit;

public class EditGroup extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.ac.GroupGeneralEditor";

	interface EditGroupUiBinder extends UiBinder<HTMLPanel, EditGroup> {
	}

	private EditGroupUiBinder binder = GWT.create(EditGroupUiBinder.class);

	public static interface EditGroupConstants extends Constants {
		String addFilter();

		String permsTab();

		String hsmTab();
	}

	@UiField
	StringEdit name;

	@UiField
	DelegationEdit delegation;

	@UiField
	TextEdit description;

	@UiField
	CheckBox defaultGroup;

	@UiField
	CheckBox hidden;

	@UiField
	CheckBox hideMembers;

	private WidgetElement widgetModel;

	// StoragePolicyEdit storagePolicy;

	public EditGroup(WidgetElement model) {
		this.widgetModel = model;
		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		name.setId("edit-group-name");
		delegation.setId("edit-group-delegation");
		description.setId("edit-group-description");

		boolean readonly = widgetModel.isReadOnly();
		name.setReadOnly(readonly);
		delegation.setReadOnly(readonly);
		description.setReadOnly(readonly);
		hidden.setEnabled(!readonly);
		hideMembers.setEnabled(!readonly);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsGroup group = map.get("group").cast();
		group.setName(name.asEditor().getValue());
		group.setDescription(description.asEditor().getValue());
		group.setHidden(hidden.getValue());
		group.setHiddenMembers(hideMembers.getValue());
		group.setOrgUnitUid(delegation.asEditor().getValue());
		if (defaultGroup.getValue() == Boolean.TRUE) {
			group.getProperties().put("is_profile", "true");
		} else {
			group.getProperties().remove("is_profile");
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsGroup group = map.get("group").cast();
		name.asEditor().setValue(group.getName());
		description.asEditor().setValue(group.getDescription());
		hidden.setValue(group.getHidden());
		hideMembers.setValue(group.getHiddenMembers());

		String isDefaultGroup = group.getProperties().get("is_profile");
		defaultGroup.setValue(isDefaultGroup != null && isDefaultGroup.equals("true"));
		delegation.setDomain(map.getString("domainUid"));
		delegation.asEditor().setValue(group.getOrgUnitUid());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, (e) -> {
			return new EditGroup(e);
		});
	}
}

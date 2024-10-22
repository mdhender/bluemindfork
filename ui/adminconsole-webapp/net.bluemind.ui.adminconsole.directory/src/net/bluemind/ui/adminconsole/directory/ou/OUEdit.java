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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class OUEdit extends CommonForm implements ICommonEditor {

	interface OUEditUiBinder extends UiBinder<HTMLPanel, OUEdit> {

	}

	private static OUEditUiBinder binder = GWT.create(OUEditUiBinder.class);

	@UiField
	TextBox name;

	private ItemValue<OrgUnit> orgUnit;

	/**
	 * Edit OU
	 * 
	 * @param orgUnitItem
	 */
	public OUEdit(OrgUnitItem orgUnitItem) {
		OrgUnit orgUnitValue = new OrgUnit();
		orgUnitValue.name = orgUnitItem.getName();
		if (!orgUnitItem.isRoot() && orgUnitItem.getParentItem() != null) {
			orgUnitValue.parentUid = orgUnitItem.getParentUid();
		}
		this.orgUnit = ItemValue.create(orgUnitItem.getUid(), orgUnitValue);
		loadUI();
	}

	private void loadUI() {
		form = binder.createAndBindUi(this);
		name.getElement().setId("ou-name");
		setFormData();
	}

	public boolean save() {
		if (validate()) {
			orgUnit.value.name = name.getText();
			return true;
		}
		return false;
	}

	private boolean validate() {
		String n = name.asEditor().getValue();
		if (n == null || n.isEmpty()) {
			Notification.get().reportError(OrgUnitConstants.INST.invalidOuName());
			return false;
		}
		return true;
	}

	private void setFormData() {
		name.setText(orgUnit.value.name);
	}

	@Override
	public Widget asWidget() {
		return form;
	}

	public ItemValue<OrgUnit> getOrgUnit() {
		return orgUnit;
	}
}

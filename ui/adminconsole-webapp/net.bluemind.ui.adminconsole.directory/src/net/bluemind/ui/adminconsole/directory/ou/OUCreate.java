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

import net.bluemind.directory.api.OrgUnit;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class OUCreate extends CommonForm implements ICommonEditor {

	interface OUCreateUiBinder extends UiBinder<HTMLPanel, OUCreate> {

	}

	private static OUCreateUiBinder binder = GWT.create(OUCreateUiBinder.class);

	@UiField
	TextBox name;

	@UiField
	OUParent parent;

	private OrgUnit orgUnit;

	private String domainUid;

	/**
	 * Create OU
	 * 
	 * @param domainUid
	 */
	public OUCreate(String domainUid) {
		this.domainUid = domainUid;
		this.orgUnit = new OrgUnit();
		loadUI();
	}

	private void loadUI() {
		form = binder.createAndBindUi(this);
		name.getElement().setId("ou-name");
		parent.getElement().setId("ou-parent");
		setFormData();
	}

	public boolean save() {
		if (validate()) {
			orgUnit.name = name.getText();
			if (!parent.getValues().isEmpty()) {
				orgUnit.parentUid = parent.getValues().iterator().next().uid;
			}
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
		parent.setDomain(domainUid);
	}

	@Override
	public Widget asWidget() {
		return form;
	}

	public OrgUnit getOrgUnit() {
		return orgUnit;
	}
}

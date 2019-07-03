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
package net.bluemind.ui.adminconsole.directory.addressbook;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.common.client.forms.StringEdit;
import net.bluemind.ui.gwtaddressbook.client.bytype.AddressbookTypeExtension;

public class EditAddressBook extends CompositeGwtWidgetElement {

	interface EditABUiBinder extends UiBinder<HTMLPanel, EditAddressBook> {
	}

	public static final String TYPE = "bm.ac.EditAddressBook";

	private EditABUiBinder binder = GWT.create(EditABUiBinder.class);

	public static interface EditGroupConstants extends Constants {
		String addFilter();

		String permsTab();

		String hsmTab();
	}

	@UiField
	StringEdit name;

	@UiField
	Label actionsHolder;

	@UiField
	DelegationEdit delegation;

	private WidgetElement widgetModel;

	public EditAddressBook(WidgetElement model) {
		this.widgetModel = model;

		HTMLPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		name.setId("edit-ab-name");
		name.setReadOnly(model.isReadOnly());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsAddressBookDescriptor ab = map.get("addressbook").cast();
		ab.setName(name.asEditor().getValue());
		ab.setOrgUnitUid(delegation.asEditor().getValue());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsAddressBookDescriptor ab = map.get("addressbook").cast();
		name.asEditor().setValue(ab.getName());
		String domainUid = map.getString("domainUid");
		delegation.setDomain(domainUid);
		delegation.asEditor().setValue(ab.getOrgUnitUid());
		if (!widgetModel.isReadOnly()) {
			String type = ab.getSettings().get("type");
			if (type == null) {
				type = "internal";
			}
			AddressbookTypeExtension ext = AddressbookTypeExtension.getExtensionByType(type);
			if (ext != null) {
				WidgetElement widget = ext.actionsWidget(map.getString("bookId"), ab.getSettings());
				widget.attach(actionsHolder.getElement());
			}
		}

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditAddressBook(e);
			}
		});
	}
}

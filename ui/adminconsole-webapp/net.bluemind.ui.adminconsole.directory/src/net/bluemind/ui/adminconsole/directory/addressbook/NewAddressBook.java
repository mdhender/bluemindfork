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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.gwtaddressbook.client.bytype.AddressbookTypeExtension;
import net.bluemind.ui.gwtaddressbook.client.bytype.CreateAddressbookWidget;

public class NewAddressBook extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateAddressBookWidget";

	private static BookUiBinder uiBinder = GWT.create(BookUiBinder.class);

	interface BookUiBinder extends UiBinder<HTMLPanel, NewAddressBook> {

	}

	private HTMLPanel dlp;

	@UiField
	Label errorLabel;

	@UiField(provided = false)
	CreateAddressbookWidget abType = new CreateAddressbookWidget();

	@UiField
	ListBox types;

	@UiField
	DelegationEdit delegation;

	private NewAddressBook() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");

		for (AddressbookTypeExtension t : abType.types()) {
			if (t.getRole() == null || Ajax.TOKEN.getRoles().contains(t.getRole())) {
				types.addItem(t.getLabel(), t.getType());
			}
		}

		types.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				abType.show(types.getSelectedValue());
			}

		});

	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			if (d.value.global) {
				errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
			} else {
				errorLabel.setText("");
			}

			JsAddressBookDescriptor ab = map.get("addressbook").cast();
			abType.show(ab.getSettings().get("type"));
			delegation.setDomain(d.uid);
		}

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JavaScriptObject t = map.get("addressbook");
		JsAddressBookDescriptor ab = t.cast();
		ab.setOrgUnitUid(delegation.asEditor().getValue());
		ab.setSystem(false);
		abType.saveModel(ab);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewAddressBook();
			}
		});
	}
}

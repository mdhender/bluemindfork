/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.gwtaddressbook.client.bytype.internal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;

public class InternalAddressbookCreationWidget extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.addressbook.InternalAddressbookCreation";

	private static InternalAddressbookCreationWidgetUiBinder uiBinder = GWT
			.create(InternalAddressbookCreationWidgetUiBinder.class);

	interface InternalAddressbookCreationWidgetUiBinder extends UiBinder<HTMLPanel, InternalAddressbookCreationWidget> {
	}

	@UiField
	TextBox label;

	private HTMLPanel form;

	public InternalAddressbookCreationWidget() {
		form = uiBinder.createAndBindUi(this);
		initWidget(form);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsAddressBookDescriptor descriptor = model.cast();
		label.setText(descriptor.getName());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsAddressBookDescriptor descriptor = model.cast();
		descriptor.setName(label.getText());
		descriptor.getSettings().put("type", "internal");
	}

	public static void registerType() {

		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				return new InternalAddressbookCreationWidget();
			}
		});
	}
}
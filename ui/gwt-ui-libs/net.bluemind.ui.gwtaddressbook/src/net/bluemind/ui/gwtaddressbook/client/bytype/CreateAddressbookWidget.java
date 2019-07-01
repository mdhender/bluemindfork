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
package net.bluemind.ui.gwtaddressbook.client.bytype;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;

public class CreateAddressbookWidget extends Composite {

	private List<AddressbookTypeExtension> extensions = new ArrayList<>();
	private WidgetElement currentWidget;
	private Label container;

	public CreateAddressbookWidget() {
		extensions = AddressbookTypeExtension.getExtensions();
		container = new Label();
		initWidget(container);
	}

	public List<AddressbookTypeExtension> types() {
		return extensions;
	}

	public void show(String type) {
		if (type == null) {
			type = "internal";
		}
		if (currentWidget != null) {
			currentWidget.detach();
			container.getElement().removeAllChildren();
		}
		AddressbookTypeExtension extension = AddressbookTypeExtension.getExtensionByType(type);

		if (extension != null) {
			currentWidget = extension.createWidget();
			currentWidget.attach(container.getElement());
		} else {
			Window.alert("nothing for type " + type);
		}
	}

	public void saveModel(JsAddressBookDescriptor desc) {
		if (currentWidget == null) {
			show(desc.getSettings().get("type"));
		}
		currentWidget.saveModel(desc);
	}
}

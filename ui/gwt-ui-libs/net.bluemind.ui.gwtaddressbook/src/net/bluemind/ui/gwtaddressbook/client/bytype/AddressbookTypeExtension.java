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

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.EditorContext;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.ui.extensions.gwt.UIExtension;
import net.bluemind.ui.extensions.gwt.UIExtensionConfigurationElement;
import net.bluemind.ui.extensions.gwt.UIExtensionPoint;
import net.bluemind.ui.extensions.gwt.UIExtensionsManager;

public class AddressbookTypeExtension {
	private UIExtensionConfigurationElement extension;

	public AddressbookTypeExtension(UIExtensionConfigurationElement extension) {
		this.extension = extension;
	}

	public String getLabel() {
		return extension.getAttribute("label");
	}

	public String getType() {
		return extension.getAttribute("type");
	}

	public String getRole() {
		return extension.getAttribute("role");
	}

	public WidgetElement createWidget() {
		String widgetName = extension.getAttribute("create-widget");
		return JsHelper.construct(WidgetElement.class, widgetName, null, EditorContext.create(Ajax.TOKEN.getRoles()));
	}

	public WidgetElement actionsWidget(String container, JsMapStringString settings) {
		JsMapStringJsObject model = JavaScriptObject.createObject().cast();
		model.putString("container", container);
		model.put("settings", settings);

		WidgetElement ret = JsHelper.construct(WidgetElement.class, extension.getAttribute("actions-widget"), model,
				EditorContext.create(Ajax.TOKEN.getRoles()));
		ret.loadModel(model);
		return ret;
	}

	public static List<AddressbookTypeExtension> getExtensions() {
		UIExtensionPoint ep = UIExtensionsManager.getInstance()
				.getExtensionPoint("net.bluemind.ui.addressbook.addressbookType");
		List<AddressbookTypeExtension> extensions = new ArrayList<>();
		for (UIExtension ext : ep.getExtensions()) {
			for (UIExtensionConfigurationElement cf : ext.getConfigurationElements("addressbook-type")) {
				extensions.add(new AddressbookTypeExtension(cf));
			}
		}
		return extensions;
	}

	public static AddressbookTypeExtension getExtensionByType(String type) {
		AddressbookTypeExtension extension = null;
		for (AddressbookTypeExtension ext : getExtensions()) {
			if (ext.getType().equals(type)) {
				extension = ext;
				break;
			}
		}
		return extension;
	}
}

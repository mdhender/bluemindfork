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
package net.bluemind.ui.gwtcalendar.client.bytype;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.EditorContext;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.ui.extensions.gwt.UIExtension;
import net.bluemind.ui.extensions.gwt.UIExtensionConfigurationElement;
import net.bluemind.ui.extensions.gwt.UIExtensionPoint;
import net.bluemind.ui.extensions.gwt.UIExtensionsManager;

public class CalendarTypeExtension {
	private UIExtensionConfigurationElement extension;

	public CalendarTypeExtension(UIExtensionConfigurationElement extension) {
		this.extension = extension;
	}

	public String getLabel() {
		return extension.getAttribute("label");
	}

	public String getType() {
		return extension.getAttribute("type");
	}

	public WidgetElement createWidget() {
		String widgetName = extension.getAttribute("create-widget");
		return JsHelper.construct(WidgetElement.class, widgetName, null, EditorContext.create(Ajax.TOKEN.getRoles()));
	}

	public WidgetElement actionsWidget(String container, JsMapStringString settings) {
		String widgetName = extension.getAttribute("actions-widget");

		JsMapStringString model = JavaScriptObject.createObject().cast();
		model.put("container", container);
		model.put("icsUrl", settings.get("icsUrl"));
		WidgetElement ret = JsHelper.construct(WidgetElement.class, widgetName, model,
				EditorContext.create(Ajax.TOKEN.getRoles()));
		ret.loadModel(model);
		return ret;
	}

	public static List<CalendarTypeExtension> getExtensions() {
		UIExtensionPoint ep = UIExtensionsManager.getInstance()
				.getExtensionPoint("net.bluemind.ui.calendar.calendarType");
		List<CalendarTypeExtension> extensions = new ArrayList<>();
		for (UIExtension ext : ep.getExtensions()) {
			for (UIExtensionConfigurationElement cf : ext.getConfigurationElements("calendar-type")) {
				extensions.add(new CalendarTypeExtension(cf));
			}
		}
		return extensions;
	}

	public static CalendarTypeExtension getExtensionByType(String type) {
		CalendarTypeExtension extension = null;
		for (CalendarTypeExtension ext : getExtensions()) {
			if (ext.getType().equals(type)) {
				extension = ext;
				break;
			}
		}
		return extension;
	}
}

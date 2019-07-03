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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;

public class CreateCalendarWidget extends Composite {

	private List<CalendarTypeExtension> extensions = new ArrayList<>();
	private WidgetElement currentWidget;
	private Label container;

	public CreateCalendarWidget() {
		extensions = CalendarTypeExtension.getExtensions();
		container = new Label();
		initWidget(container);
	}

	public List<CalendarTypeExtension> types() {
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
		CalendarTypeExtension extension = CalendarTypeExtension.getExtensionByType(type);

		if (extension != null) {
			currentWidget = extension.createWidget();
			currentWidget.attach(container.getElement());
		} else {
			Window.alert("nothing for type " + type);
		}
	}

	public void saveModel(JsCalendarDescriptor cal) {
		if (currentWidget == null) {
			show(cal.getSettings().get("type"));
		}
		currentWidget.saveModel(cal);
	}
}

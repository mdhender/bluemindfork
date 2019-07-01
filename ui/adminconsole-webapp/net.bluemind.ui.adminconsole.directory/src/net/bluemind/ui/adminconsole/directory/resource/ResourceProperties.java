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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.resource.api.gwt.js.JsResourceDescriptorPropertyValue;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property.Type;

public class ResourceProperties {

	private final String locale;
	private final HTMLPanel customPropertiesContainer;
	private final Label customPropTitle;
	private Map<String, Widget> props;
	private Map<String, String> propsValue;
	private List<Property> properties;

	public ResourceProperties(String locale, HTMLPanel customPropertiesContainer, Label customPropTitle) {
		this.locale = locale;
		this.customPropertiesContainer = customPropertiesContainer;
		this.customPropTitle = customPropTitle;
		props = new HashMap<String, Widget>();
		propsValue = new HashMap<String, String>();
	}

	public void loadProps(JsArray<JsResourceDescriptorPropertyValue> ps) {
		for (int i = 0; i < ps.length(); i++) {
			propsValue.put(ps.get(i).getPropertyId(), ps.get(i).getValue());
		}
	}

	public void load(List<Property> properties) {
		this.properties = properties;
		customPropertiesContainer.clear();
		if (properties.size() == 0) {
			customPropTitle.setVisible(false);
			return;
		}

		customPropTitle.setVisible(true);
		FlexTable customProp = new FlexTable();
		customProp.setStyleName("formContainer");
		for (Property property : properties) {

			Label label = getLabel(property);
			Widget value = getValue(propsValue, property, label);
			int row = customProp.getRowCount();

			customProp.setWidget(row, 0, label);
			customProp.setWidget(row, 1, value);

			customProp.getRowFormatter().setStyleName(row, "setting");
			customProp.getCellFormatter().setStyleName(row, 0, "label");
			customProp.getCellFormatter().setStyleName(row, 1, "form");

			props.put(property.id, value);
		}
		customPropertiesContainer.add(customProp);

	}

	public void saveValues(JsArray<JsResourceDescriptorPropertyValue> values) {
		for (Property p : properties) {
			JsResourceDescriptorPropertyValue value = JsResourceDescriptorPropertyValue.create();
			value.setPropertyId(p.id);
			value.setValue(getByType(p.type).getValue(props.get(p.id)));
			values.push(value);
		}
	}

	private Widget getValue(Map<String, String> propsValue, Property p, Label label) {
		return getByType(p.type).getWidget(propsValue, p, label);
	}

	private Label getLabel(Property property) {
		Label lbl = new Label();
		String[] labels = property.label.split("\n");
		String l = null;
		for (String label : labels) {
			if (label.startsWith(locale + "::")) {
				String[] i18nLabel = label.split("::");
				if (i18nLabel.length > 1) {
					l = i18nLabel[1];
					break;
				}
			} else {
				l = label;
			}
		}
		lbl.setText(l);
		return lbl;
	}

	private IProvidePropWidget getByType(Type type) {
		switch (type) {
		case Boolean: {
			return new BooleanPropWidget();
		}
		case Number: {
			return new NumberPropWidget();
		}
		case String:
		default: {
			return new DefaultPropWidget();
		}
		}
	}

	static interface IProvidePropWidget {
		public Widget getWidget(Map<String, String> propsValue, Property p, Label label);

		public String getValue(Widget widget);
	}

	static class BooleanPropWidget implements IProvidePropWidget {

		@Override
		public Widget getWidget(Map<String, String> propsValue, Property p, Label label) {
			CheckBox cb = new CheckBox(label.getText());
			Widget w = cb;
			String value = propsValue.get(p.id);
			if (value != null) {
				cb.setValue(value.equals("true"));
			}
			return w;
		}

		@Override
		public String getValue(Widget widget) {
			return ((CheckBox) widget).getValue().toString();
		}

	}

	static class NumberPropWidget implements IProvidePropWidget {

		@Override
		public Widget getWidget(Map<String, String> propsValue, Property p, Label label) {
			IntegerBox ib = new IntegerBox();
			ib.setValue(0);
			Widget w = ib;
			String value = propsValue.get(p.id);
			if (value != null) {
				try {
					ib.setValue(Integer.parseInt(value));
				} catch (NumberFormatException e) {
					ib.setValue(0);
				}
			}
			return w;
		}

		@Override
		public String getValue(Widget widget) {
			return ((IntegerBox) widget).getValue().toString();
		}

	}

	static class DefaultPropWidget implements IProvidePropWidget {

		@Override
		public Widget getWidget(Map<String, String> propsValue, Property p, Label label) {
			TextBox tb = new TextBox();
			Widget w = tb;
			String value = propsValue.get(p.id);
			if (value != null) {
				tb.setValue(value);
			}
			return w;
		}

		@Override
		public String getValue(Widget widget) {
			return ((TextBox) widget).getValue();
		}

	}

}

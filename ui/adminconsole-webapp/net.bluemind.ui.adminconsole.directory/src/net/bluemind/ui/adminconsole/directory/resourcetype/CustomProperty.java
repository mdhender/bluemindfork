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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;
import net.bluemind.ui.common.client.forms.I18nTextEdit;
import net.bluemind.ui.common.client.icon.Trash;

public class CustomProperty extends Composite {
	public static interface Resources extends ClientBundle {

		@Source("CustomProperty.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {
		String container();

		String trash();

	}

	private static final Resources res = GWT.create(Resources.class);

	private final Style s;

	private I18nTextEdit label;
	private ListBox type;
	private Map<String, Integer> typeList;
	private Trash trash;
	private ResourceTypeDescriptor.Property property;

	public CustomProperty() {
		s = res.editStyle();
		s.ensureInjected();
		property = new ResourceTypeDescriptor.Property();

		typeList = new HashMap<String, Integer>();

		FlexTable ft = new FlexTable();
		initWidget(ft);

		ft.setStyleName(s.container());

		label = new I18nTextEdit();
		label.setTitleText(ResourceTypeConstants.INST.label());
		type = new ListBox();

		setTypeList();

		ft.setWidget(0, 0, new Label(ResourceTypeConstants.INST.label()));
		ft.getCellFormatter().setStyleName(0, 0, "label");
		ft.setWidget(0, 1, label.getWidgetsMap().get("form"));

		ft.setWidget(1, 0, new Label(ResourceTypeConstants.INST.type()));
		ft.getCellFormatter().setStyleName(1, 0, "label");
		ft.setWidget(1, 1, type);

		trash = new Trash();
		ft.setWidget(0, 2, trash);
		ft.getCellFormatter().setStyleName(0, 2, s.trash());
		ft.getFlexCellFormatter().setRowSpan(0, 2, 2);
	}

	private void setTypeList() {
		type.addItem(ResourceTypeConstants.INST.customPropBoolean(),
				ResourceTypeDescriptor.Property.Type.Boolean.name());
		typeList.put(ResourceTypeDescriptor.Property.Type.Boolean.name(), typeList.size());

		type.addItem(ResourceTypeConstants.INST.customPropInteger(),
				ResourceTypeDescriptor.Property.Type.Number.name());
		typeList.put(ResourceTypeDescriptor.Property.Type.Number.name(), typeList.size());

		type.addItem(ResourceTypeConstants.INST.customPropText(), ResourceTypeDescriptor.Property.Type.String.name());
		typeList.put(ResourceTypeDescriptor.Property.Type.String.name(), typeList.size());
	}

	public ResourceTypeDescriptor.Property toRTCustomProperty() {
		property.label = label.getStringValue();
		property.type = ResourceTypeDescriptor.Property.Type.valueOf(type.getValue(type.getSelectedIndex()));
		return property;
	}

	public void setValues(ResourceTypeDescriptor.Property rtcp) {
		property = rtcp;
		label.setStringValue(rtcp.label);
		type.setSelectedIndex(typeList.get(rtcp.type.name()));
	}

	public Trash getTrash() {
		return trash;
	}
}

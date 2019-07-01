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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;
import net.bluemind.ui.common.client.forms.IFormChangeListener;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class CustomPropertyContainer extends Composite implements ICommonEditor {

	private FlowPanel propContainer;
	private Anchor addCustomProp;
	private List<CustomProperty> properties;

	@UiConstructor
	public CustomPropertyContainer() {
		FlowPanel container = new FlowPanel();
		initWidget(container);

		propContainer = new FlowPanel();

		addCustomProp = new Anchor(ResourceTypeConstants.INST.addCustomProp());
		addCustomProp.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				addCustomProp(null);
			}
		});

		container.add(propContainer);
		container.add(addCustomProp);

		properties = new ArrayList<>();
	}

	private void addCustomProp(ResourceTypeDescriptor.Property rtcp) {
		final CustomProperty cp = new CustomProperty();
		if (rtcp != null) {
			cp.setValues(rtcp);
		}

		propContainer.add(cp);
		properties.add(cp);

		cp.getTrash().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				propContainer.remove(cp);
				properties.remove(cp);
			}
		});

	}

	public List<ResourceTypeDescriptor.Property> getProperties() {
		List<ResourceTypeDescriptor.Property> ret = new ArrayList<>();
		for (CustomProperty p : properties) {
			ret.add(p.toRTCustomProperty());
		}
		return ret;
	}

	public void setProperties(List<ResourceTypeDescriptor.Property> properties) {

		for (ResourceTypeDescriptor.Property p : properties) {
			addCustomProp(p);
		}
	}

	@Override
	public void setTitleText(String s) {
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setDescriptionText(String s) {
	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		return null;
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}

	@Override
	public void addFormChangeListener(IFormChangeListener listener) {
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub

	}
}

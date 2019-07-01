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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;
import net.bluemind.ui.common.client.forms.StringEdit;

public class NewResourceType extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.QCreateResourceTypeWidget";

	private static NewResourceTypeUiBinder uiBinder = GWT.create(NewResourceTypeUiBinder.class);

	interface NewResourceTypeUiBinder extends UiBinder<HTMLPanel, NewResourceType> {

	}

	private HTMLPanel dlp;

	@UiField
	StringEdit name;

	@UiField
	Label errorLabel;

	private ItemValue<Domain> domain;

	private NewResourceType() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);

		// needed to embed a docklayoutpanel
		dlp.setHeight("100%");
		name.setId("new-ResourceType-name");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			updateDomainChange(d);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
			return;
		}

		String label = name.asEditor().getValue();
		if (label == null || label.trim().isEmpty()) {
			errorLabel.setText(ResourceTypeConstants.INST.emptyLabel());
			return;
		}

		JavaScriptObject t = map.get("ResourceType");
		JsResourceTypeDescriptor rt = null;
		if (t != null) {
			rt = t.cast();
		} else {
			rt = JsResourceTypeDescriptor.create();
			map.put("ResourceType", rt);
		}

		rt.setLabel(label);
	}

	@UiFactory
	ResourceTypeConstants getConstants() {
		return ResourceTypeConstants.INST;
	}

	@Override
	public void attach(Element parent) {
		super.attach(parent);
		name.setFocus(true);
	}

	private void updateDomainChange(ItemValue<Domain> active) {
		this.domain = active;
		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
		} else {
			errorLabel.setText("");
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewResourceType();
			}
		});
		GWT.log("bm.ac.QCreateResourceTypeWidget registred");
	}
}

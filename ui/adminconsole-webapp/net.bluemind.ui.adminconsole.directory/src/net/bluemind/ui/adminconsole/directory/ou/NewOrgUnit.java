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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.gwt.js.JsOrgUnit;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;

public class NewOrgUnit extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.NewOrgUnit";

	private static NewOrgUnitUiBinder uiBinder = GWT.create(NewOrgUnitUiBinder.class);

	interface NewOrgUnitUiBinder extends UiBinder<FlowPanel, NewOrgUnit> {

	}

	@UiField
	TextBox name;

	@UiField
	OUEdit parent;

	@UiField
	Label errorLabel;

	private FlowPanel panel;

	private NewOrgUnit() {

		panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
		name.getElement().setId("new-orgunit-name");
		panel.setHeight("100%");

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

			parent.setDomain(d.uid);
		}

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		JsOrgUnit ou = map.get("orgUnit").cast();
		ou.setName(name.asEditor().getValue());
		Set<OrgUnitPath> values = parent.getValues();
		if (!values.isEmpty()) {
			ou.setParentUid(values.iterator().next().uid);
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new NewOrgUnit();
			}
		});
	}
}
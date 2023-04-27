/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.ui.adminconsole.system.domains.authentication;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.authentication.AuthenticationEditorComponent;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;

public class DomainAuthenticationEditor extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.ac.DomainAuthenticationEditor";

	@UiField
	AuthenticationEditorComponent authenticationEditor;

	private static DomainAuthenticationEditorUiBinder uiBinder = GWT.create(DomainAuthenticationEditorUiBinder.class);

	interface DomainAuthenticationEditorUiBinder extends UiBinder<HTMLPanel, DomainAuthenticationEditor> {
	}

	protected DomainAuthenticationEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, w -> new DomainAuthenticationEditor());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		String domainUid = map.getString(DomainKeys.domainUid.name());

		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();
		Domain domain = new DomainGwtSerDer().deserialize(new JSONObject(jsDomain));

		authenticationEditor.load(domainUid, domain, SettingsModel.domainSettingsFrom(model));
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain domain = map.get(DomainKeys.domain.name()).cast();

		authenticationEditor.save(domain);
	}
}

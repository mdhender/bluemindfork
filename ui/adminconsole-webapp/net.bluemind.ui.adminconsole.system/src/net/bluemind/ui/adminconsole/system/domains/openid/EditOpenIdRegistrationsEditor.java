/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.ui.adminconsole.system.domains.openid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.gwt.endpoint.ExternalSystemGwtEndpoint;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class EditOpenIdRegistrationsEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.EditOpenIdRegistrationsEditor";

	@UiField
	OpenIdGrid table;

	Map<String, String> domainProperties;

	private static EditOpenIdRegistrationsEditorUiBinder uiBinder = GWT
			.create(EditOpenIdRegistrationsEditorUiBinder.class);

	interface EditOpenIdRegistrationsEditorUiBinder extends UiBinder<ResizeLayoutPanel, EditOpenIdRegistrationsEditor> {
	}

	protected EditOpenIdRegistrationsEditor() {
		ResizeLayoutPanel panel = uiBinder.createAndBindUi(this);
		panel.setHeight("100%");
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, w -> new EditOpenIdRegistrationsEditor());
	}

	@Override
	public void show() {
		super.show();
		table.onResize();
	}

	@Override
	public void loadModel(JavaScriptObject model) {

		JsMapStringJsObject map = model.cast();
		JsDomain jsdomain = map.get(DomainKeys.domain.name()).cast();
		domainProperties = jsdomain.getProperties().asMap();

		new ExternalSystemGwtEndpoint(Ajax.TOKEN.getSessionId())
				.getExternalSystems(new DefaultAsyncHandler<List<ExternalSystem>>() {

					@Override
					public void success(List<ExternalSystem> value) {
						List<OpenIdRegistration> list = new ArrayList<>();

						for (ExternalSystem system : value) {
							if (system.authKind.name().startsWith("OPEN_ID")) {
								String endpointKey = system.identifier + "_endpoint";
								String applicationIdKey = system.identifier + "_appid";
								String applicationSecretKey = system.identifier + "_secret";
								String tokenEndpointKey = system.identifier + "_tokenendpoint";

								String endpoint = domainProperties.get(endpointKey) != null
										? domainProperties.get(endpointKey)
										: "";
								String applicationId = domainProperties.get(applicationIdKey) != null
										? domainProperties.get(applicationIdKey)
										: "";
								String applicationSecret = domainProperties.get(applicationSecretKey) != null
										? domainProperties.get(applicationSecretKey)
										: "";
								String tokenEndpoint = domainProperties.get(tokenEndpointKey) != null
										? domainProperties.get(tokenEndpointKey)
										: "";
								OpenIdRegistration registration = new OpenIdRegistration(system.identifier, endpoint,
										applicationId, applicationSecret, tokenEndpoint);
								list.add(registration);
							}
						}
						table.setValues(list);
						table.setHeight("100%");
					}

				});
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		table.getValues().forEach(system -> {
			String endpointKey = system.systemIdentifier + "_endpoint";
			String applicationIdKey = system.systemIdentifier + "_appid";
			String applicationSecretKey = system.systemIdentifier + "_secret";
			String tokenEndpointKey = system.systemIdentifier + "_tokenendpoint";

			domainProperties.put(endpointKey, system.endpoint);
			domainProperties.put(applicationIdKey, system.applicationId);
			domainProperties.put(applicationSecretKey, system.applicationSecret);
			domainProperties.put(tokenEndpointKey, system.tokenEndpoint);
			JsMapStringString props = JsMapStringString.create(domainProperties);

			JsMapStringJsObject map = model.cast();
			JsDomain jsdomain = map.get(DomainKeys.domain.name()).cast();
			jsdomain.setProperties(props);
			map.put(DomainKeys.domain.name(), jsdomain);
		});
	}

}

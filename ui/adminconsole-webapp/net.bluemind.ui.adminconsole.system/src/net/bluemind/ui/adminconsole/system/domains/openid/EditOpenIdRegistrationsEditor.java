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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ResizeLayoutPanel;

import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.gwt.endpoint.ExternalSystemGwtEndpoint;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.common.client.forms.Ajax;

public class EditOpenIdRegistrationsEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.EditOpenIdRegistrationsEditor";

	@UiField
	OpenIdGrid table;

	SettingsModel domainSettings;

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

		domainSettings = SettingsModel.domainSettingsFrom(model);

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

								String endpoint = domainSettings.get(endpointKey) != null
										? domainSettings.get(endpointKey)
										: "";
								String applicationId = domainSettings.get(applicationIdKey) != null
										? domainSettings.get(applicationIdKey)
										: "";
								String applicationSecret = domainSettings.get(applicationSecretKey) != null
										? domainSettings.get(applicationSecretKey)
										: "";
								String tokenEndpoint = domainSettings.get(tokenEndpointKey) != null
										? domainSettings.get(tokenEndpointKey)
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

			domainSettings.putString(endpointKey, system.endpoint);
			domainSettings.putString(applicationIdKey, system.applicationId);
			domainSettings.putString(applicationSecretKey, system.applicationSecret);
			domainSettings.putString(tokenEndpointKey, system.tokenEndpoint);

		});
	}

}

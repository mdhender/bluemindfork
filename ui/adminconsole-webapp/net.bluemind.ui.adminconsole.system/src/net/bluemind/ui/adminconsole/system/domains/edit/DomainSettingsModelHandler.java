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
package net.bluemind.ui.adminconsole.system.domains.edit;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainSettingsModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.DomainSettingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainSettingsModelHandler();
			}
		});
		GWT.log("bm.ac.DomainSettingsModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());

		SettingsModel domainSettingsModel = SettingsModel.domainSettingsFrom(model);

		DomainSettingsGwtEndpoint domainSettings = new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		domainSettings.get(new DefaultAsyncHandler<Map<String, String>>(handler) {

			@Override
			public void success(Map<String, String> values) {
				for (String key : values.keySet()) {
					domainSettingsModel.setValue(key, values.get(key));
				}
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());

		SettingsModel domainSettingsModel = SettingsModel.domainSettingsFrom(model);

		final Map<String, String> domainSettings = new HashMap<>();

		for (String k : domainSettingsModel.keys()) {
			domainSettings.put(k, domainSettingsModel.getValue(k));
		}

		DomainSettingsGwtEndpoint domainSettingsService = new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(),
				domainUid);
		domainSettingsService.set(domainSettings, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				handler.success(null);
			}
		});

	}
}

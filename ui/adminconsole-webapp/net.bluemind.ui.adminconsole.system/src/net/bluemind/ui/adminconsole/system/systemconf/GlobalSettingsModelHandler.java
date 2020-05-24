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
package net.bluemind.ui.adminconsole.system.systemconf;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.gwt.endpoint.GlobalSettingsGwtEndpoint;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.common.client.forms.Ajax;

public class GlobalSettingsModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.GlobalSettingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new GlobalSettingsModelHandler();
			}
		});
		GWT.log("bm.ac.GlobalSettingsModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SettingsModel globalSettingsModel = SettingsModel.globalSettingsFrom(model);
		GlobalSettingsGwtEndpoint settings = new GlobalSettingsGwtEndpoint(Ajax.TOKEN.getSessionId());
		settings.get(new DefaultAsyncHandler<Map<String, String>>() {

			@Override
			public void success(Map<String, String> values) {
				for (String key : values.keySet()) {
					globalSettingsModel.setValue(key, values.get(key));
				}
				handler.success(null);
			}
		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SettingsModel globalSettingsModel = SettingsModel.globalSettingsFrom(model);

		final Map<String, String> settings = new HashMap<>();

		for (String k : globalSettingsModel.keys()) {
			settings.put(k, globalSettingsModel.getValue(k));
		}

		new GlobalSettingsGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi().set(settings).thenAccept(v -> {
			Notification.get().reportInfo("Global settings configuration saved");
			handler.success(null);
		}).exceptionally(e -> {
			handler.failure(e);
			return null;
		});

	}

}

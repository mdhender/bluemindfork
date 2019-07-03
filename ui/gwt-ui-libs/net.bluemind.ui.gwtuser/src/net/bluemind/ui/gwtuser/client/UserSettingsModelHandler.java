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
package net.bluemind.ui.gwtuser.client;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.gwt.endpoint.UserSettingsGwtEndpoint;

public class UserSettingsModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.user.UserSettingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new UserSettingsModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("userId");
		String domainUid = map.getString("domainUid");
		UserSettingsGwtEndpoint settings = new UserSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		settings.get(s, new AsyncHandler<Map<String, String>>() {

			@Override
			public void success(Map<String, String> value) {

				map.put("user-settings", JsMapStringString.create(value));
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("userId");
		String domainUid = map.getString("domainUid");

		JsMapStringString settingsValues = map.get("user-settings").cast();

		UserSettingsGwtEndpoint settings = new UserSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		settings.set(s, settingsValues.asMap(), handler);
	}

}

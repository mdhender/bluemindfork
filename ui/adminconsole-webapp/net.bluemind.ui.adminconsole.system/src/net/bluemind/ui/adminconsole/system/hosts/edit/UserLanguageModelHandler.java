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
package net.bluemind.ui.adminconsole.system.hosts.edit;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.gwt.endpoint.UserSettingsGwtEndpoint;

public class UserLanguageModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.UserLanguageModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new UserLanguageModelHandler();
			}
		});
		GWT.log("bm.ac.UserLanguageModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		UserSettingsGwtEndpoint userSettingsService = new UserSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid());
		userSettingsService.get(Ajax.TOKEN.getSubject(), new DefaultAsyncHandler<Map<String, String>>(handler) {

			@Override
			public void success(Map<String, String> value) {
				String lang = value.get("lang");
				JSONObject langObject = new JSONObject();
				langObject.put(HostKeys.lang.name(), new JSONString(lang));
				map.put(HostKeys.lang.name(), langObject.getJavaScriptObject());
				handler.success(null);
			}
		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		handler.success(null);
	}

}

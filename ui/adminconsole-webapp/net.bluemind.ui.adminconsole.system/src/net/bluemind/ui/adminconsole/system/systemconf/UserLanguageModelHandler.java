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

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
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
				map.put(HostKeys.lang.name(), value.get("lang"));
				handler.success(null);
			}
		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		UserSettingsGwtEndpoint userSettingsService = new UserSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid());

		String lang = map.getString(HostKeys.lang.name());
		userSettingsService.setOne(Ajax.TOKEN.getSubject(), HostKeys.lang.name(), lang,
				new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
						Notification.get().reportInfo("Admin language updated");
						handler.success(null);
					}
				});
	}

}

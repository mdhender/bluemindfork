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
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.api.gwt.endpoint.SystemConfigurationGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public class SysConfModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.SysConfModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new SysConfModelHandler();
			}
		});
		GWT.log("bm.ac.SysConfModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SystemConfigurationGwtEndpoint config = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
		config.getValues(new DefaultAsyncHandler<SystemConf>(handler) {

			@Override
			public void success(SystemConf value) {
				final Map<String, String> values = value.values;

				SysConfModel sysConfModel = SysConfModel.from(model);
				for (Map.Entry<String, String> entry : values.entrySet()) {
					sysConfModel.setValue(entry.getKey(), entry.getValue());
				}
				handler.success(null);

			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SystemConfigurationGwtEndpoint config = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());

		SysConfModel sysConfModel = SysConfModel.from(model);

		HashMap<String, String> values = new HashMap<>();
		for (String k : sysConfModel.keys()) {
			values.put(k, sysConfModel.getValue(k));
		}

		String passwd = values.get(SysConfKeys.sw_password.name());
		if (passwd == null || passwd.trim().isEmpty()) {
			Notification.get().reportError("SW password cannot be empty");
			handler.success(null);
		} else {
			config.updateMutableValues(values, new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
					Notification.get().reportInfo("Saved system configuration");
					handler.success(null);
				}
			});
		}
	}

}

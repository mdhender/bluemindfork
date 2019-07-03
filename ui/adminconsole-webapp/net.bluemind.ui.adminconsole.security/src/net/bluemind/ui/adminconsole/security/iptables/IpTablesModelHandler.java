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
package net.bluemind.ui.adminconsole.security.iptables;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
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

public class IpTablesModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.IpTablesModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new IpTablesModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		SystemConfigurationGwtEndpoint config = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
		config.getValues(new DefaultAsyncHandler<SystemConf>(handler) {

			@Override
			public void success(SystemConf value) {
				map.put("sysconf", JsMapStringString.create(value.values));
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		SystemConfigurationGwtEndpoint config = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
		Map<String, String> values = collectValues(model);
		config.updateMutableValues(values, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo("Saved system configuration");
				handler.success(null);
			}
		});
	}

	private Map<String, String> collectValues(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsMapStringString jsValues = map.get("sysconf").cast();

		Map<String, String> values = new HashMap<>();
		values.put(SysConfKeys.fwAdditionalIPs.name(), jsValues.get(SysConfKeys.fwAdditionalIPs.name()));
		return values;
	}

}

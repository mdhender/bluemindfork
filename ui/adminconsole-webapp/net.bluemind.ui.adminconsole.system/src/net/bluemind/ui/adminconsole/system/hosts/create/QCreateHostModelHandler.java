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
package net.bluemind.ui.adminconsole.system.hosts.create;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.WaitForTaskRefHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.server.api.gwt.serder.ServerGwtSerDer;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateHostModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.QCreateHostModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateHostModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateHostModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		handler.success(null);
	}

	@Override
	public void save(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		ServerGwtEndpoint service = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		JsServer server = map.get(HostKeys.server.name()).cast();
		Server serverInstance = new ServerGwtSerDer().deserialize(new JSONObject(server));
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid().toLowerCase();

		service.create(uid, serverInstance, new WaitForTaskRefHandler() {

			@Override
			public void onSuccess(TaskStatus status) {
				Notification.get().reportInfo(HostConstants.INST.hostCreated());
				map.putString(HostKeys.host.name(), uid);
				handler.success(null);
			}

			@Override
			public void onFailure(TaskStatus status) {
				handler.failure(
						new RuntimeException(HostConstants.INST.hostCreatedFailure() + " : " + status.lastLogEntry));

			}
		});
	}

}

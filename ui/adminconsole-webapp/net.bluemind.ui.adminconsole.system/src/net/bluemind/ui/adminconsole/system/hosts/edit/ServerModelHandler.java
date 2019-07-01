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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.handler.WaitForTaskRefHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.server.api.gwt.serder.ServerGwtSerDer;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class ServerModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.ServerModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new ServerModelHandler();
			}
		});
		GWT.log("bm.ac.ServerModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String serverId = map.getString(HostKeys.host.name());
		GWT.log("serverid: " + serverId);
		ServerGwtEndpoint service = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		service.getComplete(serverId, new DefaultAsyncHandler<ItemValue<Server>>(handler) {

			@Override
			public void success(ItemValue<Server> value) {
				JsServer server = new ServerGwtSerDer().serialize(value.value).isObject().getJavaScriptObject().cast();
				map.put(HostKeys.server.name(), server);
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		ServerGwtEndpoint service = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");

		String serverId = map.getString(HostKeys.host.name());
		JsServer jsServer = map.get(HostKeys.server.name()).cast();
		Server server = new ServerGwtSerDer().deserialize(new JSONObject(jsServer));

		JSONArray tags = new JSONArray(map.get(HostKeys.tags.name()));
		server.tags = tagsToList(tags);

		service.update(serverId, server, new WaitForTaskRefHandler() {
			@Override
			public void onSuccess(TaskStatus status) {
				Notification.get().reportInfo(HostConstants.INST.hostUpdated());
				handler.success(null);
			}

			@Override
			public void onFailure(TaskStatus status) {
				handler.failure(
						new RuntimeException(HostConstants.INST.hostUpdatedFailure() + " : " + status.lastLogEntry));
			}
		});

	}

	private List<String> tagsToList(JSONArray tags) {
		List<String> tagList = new ArrayList<>();
		for (int i = 0; i < tags.size(); i++) {
			String tag = tags.get(i).toString().replaceAll("\"", "");
			tagList.add(tag);
		}
		return tagList;
	}

}

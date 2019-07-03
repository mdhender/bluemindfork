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

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.server.api.gwt.serder.ServerGwtSerDer;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class ServersModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.ServersModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new ServersModelHandler();
			}
		});
		GWT.log("bm.ac.ServersModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final ServerGwtEndpoint serverService = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		serverService.allComplete(new DefaultAsyncHandler<List<ItemValue<Server>>>(handler) {

			@Override
			public void success(List<ItemValue<Server>> servers) {
				setServers(map, servers);
				handler.success(null);
			}

			private void setServers(final JsMapStringJsObject map, List<ItemValue<Server>> servers) {
				map.put(DomainKeys.allServers.name(),
						new GwtSerDerUtils.ListSerDer<>(new ItemValueGwtSerDer<>(new ServerGwtSerDer()))
								.serialize(servers).isArray().getJavaScriptObject());
			}
		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		handler.success(null);
	}

}

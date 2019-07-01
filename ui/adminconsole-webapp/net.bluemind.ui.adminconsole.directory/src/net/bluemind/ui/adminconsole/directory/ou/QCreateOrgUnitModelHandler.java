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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.directory.api.gwt.js.JsOrgUnit;
import net.bluemind.directory.api.gwt.serder.OrgUnitGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateOrgUnitModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateOrgUnitModelHandler";

	private QCreateOrgUnitModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateOrgUnitModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		JsMapStringJsObject map = model.cast();
		map.put("orgUnit", JsOrgUnit.create());
		handler.success(null);
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();

		String domainUid = map.getString("domainUid");
		if ("global.virt".equals(domainUid)) {
			handler.failure(null);
			return;
		}

		JsOrgUnit ou = map.get("orgUnit").cast();
		IOrgUnitsPromise ous = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		CompletableFuture<Void> cf = ous.create(UIDGenerator.uid(),
				new OrgUnitGwtSerDer().deserialize(new JSONObject(ou)));

		cf.thenAccept(v -> handler.success(null)).exceptionally(t -> {
			handler.failure(t);
			return null;
		});
	}
}

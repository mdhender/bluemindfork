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
package net.bluemind.ui.adminconsole.directory.externaluser;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.externaluser.api.ExternalUser;
import net.bluemind.externaluser.api.gwt.endpoint.ExternalUserGwtEndpoint;
import net.bluemind.externaluser.api.gwt.js.JsExternalUser;
import net.bluemind.externaluser.api.gwt.serder.ExternalUserGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateExternalUserModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateExternalUserModelHandler";

	private QCreateExternalUserModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
						return new QCreateExternalUserModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateExternalUserModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {
		GWT.log("initialize QCreateExternalUserModel");
		JsMapStringJsObject map = model.cast();
		ExternalUser eu = new ExternalUser();
		map.put("externaluser", new ExternalUserGwtSerDer().serialize(eu)
				.isObject()
				.getJavaScriptObject());
		handler.success(null);
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();

		String domainUid = map.getString("domainUid");
		JsExternalUser externalUser = map.get("externaluser").cast();

		final ExternalUserGwtEndpoint externalUsers = new ExternalUserGwtEndpoint(
				Ajax.TOKEN.getSessionId(), domainUid);
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("externalUserUid", uid);
		externalUsers.create(uid,
				new ExternalUserGwtSerDer()
						.deserialize(new JSONObject(externalUser)),
				handler);
	}

}

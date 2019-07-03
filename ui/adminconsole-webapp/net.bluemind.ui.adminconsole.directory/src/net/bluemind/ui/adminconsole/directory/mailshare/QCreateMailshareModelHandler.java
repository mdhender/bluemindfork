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
package net.bluemind.ui.adminconsole.directory.mailshare;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.mailshare.api.gwt.endpoint.MailshareGwtEndpoint;
import net.bluemind.mailshare.api.gwt.js.JsMailshare;
import net.bluemind.mailshare.api.gwt.serder.MailshareGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateMailshareModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateMailshareModelHandler";

	private QCreateMailshareModelHandler() {
	}

	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		JsItemValue<JsDomain> domain = map.get("domain").cast();
		JsMailshare mailshare = map.get("mailshare").cast();
		mailshare.setArchived(false);
		MailshareGwtEndpoint mailshares = new MailshareGwtEndpoint(Ajax.TOKEN.getSessionId(), domain.getUid());
		String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("id", uid);

		Mailshare ms = new MailshareGwtSerDer().deserialize(new JSONObject(mailshare));
		if (ms.emails.isEmpty()) {
			ms.routing = Routing.none;
		}

		mailshares.create(uid, ms, handler);

	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateMailshareModel");
		JsMapStringJsObject map = model.cast();
		Mailshare mailshare = new Mailshare();
		map.put("mailshare", new MailshareGwtSerDer().serialize(mailshare).isObject().getJavaScriptObject());
		handler.success(null);
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateMailshareModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateMailshareModelHandler registred");
	}

}

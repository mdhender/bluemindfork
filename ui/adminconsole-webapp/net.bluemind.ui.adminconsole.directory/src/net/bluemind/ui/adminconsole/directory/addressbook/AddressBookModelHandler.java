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
package net.bluemind.ui.adminconsole.directory.addressbook;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.gwt.endpoint.AddressBooksMgmtGwtEndpoint;
import net.bluemind.addressbook.api.gwt.js.JsAddressBookDescriptor;
import net.bluemind.addressbook.api.gwt.serder.AddressBookDescriptorGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class AddressBookModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.AddressBookModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new AddressBookModelHandler();
			}
		});
		GWT.log("bm.ac.AddressBookModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("bookId");
		// String domainUid = map.getString("domainUid");

		AddressBooksMgmtGwtEndpoint dab = new AddressBooksMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());

		dab.getComplete(s, new DefaultAsyncHandler<AddressBookDescriptor>(handler) {

			@Override
			public void success(AddressBookDescriptor value) {
				map.put("addressbook",
						new AddressBookDescriptorGwtSerDer().serialize(value).isObject().getJavaScriptObject());
				parentHandler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("bookId");
		// String domainUid = map.getString("domainUid");

		AddressBooksMgmtGwtEndpoint dab = new AddressBooksMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());

		JsAddressBookDescriptor ab = map.get("addressbook").cast();
		dab.update(s, new AddressBookDescriptorGwtSerDer().deserialize(new JSONObject(ab)), handler);
	}

}

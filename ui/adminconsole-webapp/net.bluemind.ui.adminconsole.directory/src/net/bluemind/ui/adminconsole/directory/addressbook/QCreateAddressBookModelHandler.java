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
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateAddressBookModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateAddressBookModelHandler";

	private QCreateAddressBookModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateAddressBookModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateAddressBookModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateAddressBookModel");
		JsMapStringJsObject map = model.cast();

		AddressBookDescriptor ab = new AddressBookDescriptor();
		map.put("addressbook", new AddressBookDescriptorGwtSerDer().serialize(ab).isObject().getJavaScriptObject());

		handler.success(null);
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();

		String domainUid = map.getString("domainUid");
		JsAddressBookDescriptor book = map.get("addressbook").cast();
		book.setOwner(domainUid);
		book.setDomainUid(domainUid);
		AddressBooksMgmtGwtEndpoint dab = new AddressBooksMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("bookUid", uid);
		dab.create(uid, new AddressBookDescriptorGwtSerDer().deserialize(new JSONObject(book)), false, handler);
	}

}

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

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.addressbook.api.gwt.js.JsVCard;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.externaluser.api.IExternalUserPromise;
import net.bluemind.externaluser.api.gwt.endpoint.ExternalUserGwtEndpoint;
import net.bluemind.externaluser.api.gwt.js.JsExternalUser;
import net.bluemind.externaluser.api.gwt.serder.ExternalUserGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class ExternalUserModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.ExternalUserModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE,
				new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

					@Override
					public IGwtModelHandler create(ModelHandler modelHandler) {
						return new ExternalUserModelHandler();
					}
				});
		GWT.log("bm.ac.ExternalUserModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("externalUserId");
		String domainUid = map.getString("domainUid");
		IExternalUserPromise externalUsers = new ExternalUserGwtEndpoint(
				Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		CompletableFuture<Void> externalUserLoad = externalUsers.getComplete(s)
				.thenAccept(value -> {
					JsExternalUser externalUser = new ExternalUserGwtSerDer()
							.serialize(value.value).isObject()
							.getJavaScriptObject().cast();
					map.put("externaluser", externalUser);
					map.put("vcard", externalUser.getContactInfos());
					map.put("dirItem",
							new ItemValueGwtSerDer<>(
									new ExternalUserGwtSerDer())
											.serialize(value).isObject()
											.getJavaScriptObject().cast());
				});

		CompletableFuture.allOf(externalUserLoad)
				.thenRun(() -> handler.success(null)).exceptionally(t -> {
					handler.failure(t);
					return null;
				});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("externalUserId");
		String domainUid = map.getString("domainUid");
		ExternalUserGwtEndpoint externalUsers = new ExternalUserGwtEndpoint(
				Ajax.TOKEN.getSessionId(), domainUid);

		JsExternalUser externalUser = map.get("externaluser").cast();
		
		JsVCard vcard = map.get("vcard").cast();
		// because external user sanitizer overwrites directory email with vcard email (desired operation)
		vcard.getCommunications().getEmails().get(0).setValue(externalUser.getEmails().get(0).getAddress());
		
		externalUser.setContactInfos(vcard);
				
		externalUsers.update(s, new ExternalUserGwtSerDer().deserialize(
				new JSONObject(externalUser.<JavaScriptObject>cast())),
				new DefaultAsyncHandler<Void>(handler) {
					@Override
					public void success(Void value) {
						handler.success(null);
					}

					@Override
					public void failure(Throwable e) {
						handler.failure(e);
					}
				});
	}
}

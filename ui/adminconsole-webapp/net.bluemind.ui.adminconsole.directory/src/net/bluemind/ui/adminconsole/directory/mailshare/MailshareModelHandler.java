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

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettingsPromise;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsEndpointPromise;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsSockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.mailshare.api.IMailsharePromise;
import net.bluemind.mailshare.api.gwt.endpoint.MailshareGwtEndpoint;
import net.bluemind.mailshare.api.gwt.js.JsMailshare;
import net.bluemind.mailshare.api.gwt.serder.MailshareGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;

public class MailshareModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.MailshareModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new MailshareModelHandler();
			}
		});
		GWT.log("bm.ac.GroupModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("mailshareId");
		String domainUid = map.getString("domainUid");

		IDomainSettingsPromise domainSettings = new DomainSettingsEndpointPromise(
				new DomainSettingsSockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid));
		CompletableFuture<Void> domainSettingsLoad = domainSettings.get().thenAccept(value -> {
			String key = DomainSettingsKeys.mail_routing_relay.name();
			String v = value.get(key);
			String val = null != v ? v : "";
			map.putString(key, val);
		});

		IMailsharePromise mailshares = new MailshareGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		CompletableFuture<Void> mailshareLoad = mailshares.getComplete(s).thenAccept(ms -> {
			if (ms == null) {
				throw new RuntimeException("mailshare " + s + " not found");
			}
			JsMailshare mailshare = new MailshareGwtSerDer().serialize(ms.value).isObject().getJavaScriptObject()
					.cast();
			map.put("mailshare", mailshare);
			map.put("vcard", mailshare.getCard());
			handler.success(null);
		});

		CompletableFuture.allOf(mailshareLoad, domainSettingsLoad).thenRun(() -> handler.success(null))
				.exceptionally(t -> {
					handler.failure(t);
					return null;
				});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("mailshareId");
		String domainUid = map.getString("domainUid");
		IMailsharePromise mailshares = new MailshareGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		JsMailshare mailshare = map.get("mailshare").cast();
		mailshare.setCard(map.get("vcard").cast());
		mailshares.update(s, new MailshareGwtSerDer().deserialize(new JSONObject(mailshare.<JavaScriptObject>cast())))
				.thenCompose((v) -> {
					if (map.getString("vcardPhoto") != null) {
						return mailshares.setPhoto(s, btoa(map.getString("vcardPhoto")).getBytes());
					} else if (map.getString("deletePhoto") != null) {
						return mailshares.deletePhoto(s);
					} else {
						return CompletableFuture.completedFuture(null);
					}
				}).thenAccept((v) -> {
					handler.success(null);
				}).exceptionally((e) -> {
					handler.failure(e);
					return null;
				});
	}

	native String btoa(String b64)
	/*-{
	return btoa(b64);
	}-*/;

}

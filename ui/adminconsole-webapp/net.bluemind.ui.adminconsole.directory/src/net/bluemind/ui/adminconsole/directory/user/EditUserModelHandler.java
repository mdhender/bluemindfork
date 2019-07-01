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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.gwt.js.JsVCard;
import net.bluemind.addressbook.api.gwt.serder.VCardGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettingsPromise;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsEndpointPromise;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsSockJsEndpoint;
import net.bluemind.eas.api.IEasPromise;
import net.bluemind.eas.api.gwt.endpoint.EasEndpointPromise;
import net.bluemind.eas.api.gwt.endpoint.EasSockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;
import net.bluemind.user.api.gwt.js.JsUser;
import net.bluemind.user.api.gwt.serder.UserGwtSerDer;

public class EditUserModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.EditUserModelHandler";
	private boolean canManageUser = false;

	protected EditUserModelHandler(ModelHandler modelHandler) {
		JsArrayString roles = modelHandler.getActiveRoles();
		for (int i = 0; i < roles.length(); i++) {
			if (BasicRoles.ROLE_MANAGE_USER.equals(roles.get(i))) {
				canManageUser = true;
			}
		}
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new EditUserModelHandler(modelHandler);
			}
		});
		GWT.log("bm.ac.EditUserModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String userUid = map.getString("userId");
		final String domainUid = map.getString("domainUid");

		IEasPromise easConf = new EasEndpointPromise(new EasSockJsEndpoint(Ajax.TOKEN.getSessionId()));

		CompletableFuture<Void> easConfLoad = easConf.getConfiguration().thenAccept(value -> {
			map.putString(SysConfKeys.eas_sync_unknown.name(), value.get(SysConfKeys.eas_sync_unknown.name()));
		});

		IUserPromise users = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		CompletableFuture<Void> userLoad = users.getComplete(userUid).thenAccept(value -> {
			if (value.value.contactInfos == null) {
				value.value.contactInfos = new VCard();
			}
			JsUser user = new UserGwtSerDer().serialize(value.value).isObject().getJavaScriptObject().cast();
			map.put("user", user);
			map.put("vcard", user.getContactInfos());
			map.put("dirItem", new ItemValueGwtSerDer<>(new UserGwtSerDer()).serialize(value).isObject()
					.getJavaScriptObject().cast());
		});

		IDomainSettingsPromise domainSettings = new DomainSettingsEndpointPromise(
				new DomainSettingsSockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid));

		CompletableFuture<Void> domainSettingsLoad = domainSettings.get().thenAccept(value -> {
			String key = DomainSettingsKeys.mail_routing_relay.name();
			String v = value.get(key);
			String val = null != v ? v : "";
			map.putString(key, val);

		});

		CompletableFuture.allOf(easConfLoad, userLoad, domainSettingsLoad).thenRun(() -> handler.success(null))
				.exceptionally(t -> {
					handler.failure(t);
					return null;
				});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		if (canManageUser) {

			final JsMapStringJsObject map = model.cast();
			final String s = map.getString("userId");
			final String domainUid = map.getString("domainUid");
			IUserPromise users = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
			JsItemValue<JsUser> itemUser = map.get("dirItem").cast();

			JsUser user = map.get("user").cast();
			user.setContactInfos(map.get("vcard").cast());

			CompletableFuture<Void> updateExtId = null;
			String extId = map.getString("updateExtId");
			if ((extId != itemUser.getExternalId()) || (extId != null && !extId.equals(itemUser.getExternalId()))) {
				updateExtId = users.setExtId(s, extId);
			} else {
				updateExtId = CompletableFuture.completedFuture(null);
			}

			updateExtId.thenCompose(v -> {
				return users.update(s, new UserGwtSerDer().deserialize(new JSONObject(user)));
			}).thenCompose(v -> {
				if (map.getString("vcardPhoto") != null) {
					return users.setPhoto(s, btoa(map.getString("vcardPhoto")).getBytes());
				} else if (map.getString("deletePhoto") != null) {
					return users.deletePhoto(s);
				} else {
					return CompletableFuture.completedFuture(null);
				}
			}).thenAccept(v -> {
				handler.success(null);
			}).exceptionally(e -> {
				handler.failure(e);
				return null;
			});
		} else {

			final JsMapStringJsObject map = model.cast();
			final String s = map.getString("userId");
			final String domainUid = map.getString("domainUid");
			IUserPromise users = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

			JsVCard userVCard = map.get("vcard").cast();

			users.updateVCard(s, new VCardGwtSerDer().deserialize(new JSONObject(userVCard))).thenCompose(v -> {
				if (map.getString("vcardPhoto") != null) {
					return users.setPhoto(s, btoa(map.getString("vcardPhoto")).getBytes());
				} else if (map.getString("deletePhoto") != null) {
					return users.deletePhoto(s);
				} else {
					return CompletableFuture.completedFuture(null);
				}
			}).thenAccept(v -> {
				handler.success(null);
			}).exceptionally(e -> {
				handler.failure(e);
				return null;
			});

		}
	}

	native String btoa(String b64)
	/*-{
	return btoa(b64);
	}-*/;

}

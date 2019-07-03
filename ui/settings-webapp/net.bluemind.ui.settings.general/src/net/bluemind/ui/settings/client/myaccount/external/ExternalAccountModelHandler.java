/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.ui.settings.client.myaccount.external;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.system.api.IExternalSystemPromise;
import net.bluemind.system.api.gwt.endpoint.ExternalSystemGwtEndpoint;
import net.bluemind.system.api.gwt.js.JsExternalSystem;
import net.bluemind.system.api.gwt.serder.ExternalSystemGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserExternalAccountPromise;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;
import net.bluemind.user.api.gwt.endpoint.UserExternalAccountGwtEndpoint;
import net.bluemind.user.api.gwt.js.JsUserAccountInfo;
import net.bluemind.user.api.gwt.serder.UserAccountGwtSerDer;
import net.bluemind.user.api.gwt.serder.UserAccountInfoGwtSerDer;

public class ExternalAccountModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.user.ExternalAccountModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new ExternalAccountModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		IExternalSystemPromise service = new ExternalSystemGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		IUserExternalAccountPromise accountService = new UserExternalAccountGwtEndpoint(Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid(), Ajax.TOKEN.getSubject()).promiseApi();

		List<CompletableFuture<CompleteExternalSystem>> systems = new ArrayList<>();
		CompletableFuture<Void> call = service.getExternalSystems().thenAccept(list -> {
			list.forEach(extSystem -> {
				systems.add(service.getLogo(extSystem.identifier).thenApply(logo -> {
					String icon = getIcon(extSystem.identifier, logo);
					return new CompleteExternalSystem(extSystem, icon);
				}));
			});
		});

		call.thenAccept((v -> {
			CompletableFuture<List<UserAccountInfo>> accounts = accountService.getAll();
			CompletableFuture<List<CompleteExternalSystem>> externalSystems = all(systems);
			CompletableFuture.allOf(accounts, externalSystems).thenRun(() -> {
				JsArray<JsCompleteExternalSystem> systemArray = JsArray.createArray().cast();
				int index = 0;
				for (CompleteExternalSystem system : externalSystems.join()) {
					JsExternalSystem jsSystem = new ExternalSystemGwtSerDer().serialize(system).isObject()
							.getJavaScriptObject().cast();
					JsCompleteExternalSystem complete = JsCompleteExternalSystem.create();
					complete.setSystem(jsSystem, system.logo);
					systemArray.set(index++, complete);
				}
				map.put("external-systems", systemArray);
				JsArray<JsUserAccountInfo> accountArray = JsArray.createArray().cast();
				index = 0;
				for (UserAccountInfo account : accounts.join()) {
					JsUserAccountInfo jsAccount = new UserAccountInfoGwtSerDer().serialize(account).isObject()
							.getJavaScriptObject().cast();
					accountArray.set(index, jsAccount);
				}
				map.put("external-accounts", accountArray);

				handler.success(null);
			});
		})).exceptionally(e -> {
			handler.failure(e);
			return null;
		});

	}

	private <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
				.thenApply((v) -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		IUserExternalAccountPromise accountService = new UserExternalAccountGwtEndpoint(Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid(), Ajax.TOKEN.getSubject()).promiseApi();

		JsArray<JsUserAccountInfo> createdAccounts = map.get("ext-accounts-created").cast();
		JsArray<JsUserAccountInfo> modifiedAccounts = map.get("ext-accounts-modified").cast();
		JsArrayString deletedAccounts = map.get("ext-accounts-deleted").cast();

		List<CompletableFuture<Void>> done = new ArrayList<>();

		for (int i = 0; i < createdAccounts.length(); i++) {
			JsUserAccountInfo account = createdAccounts.get(i);
			UserAccount deserialized = new UserAccountGwtSerDer().deserialize(new JSONObject(account));
			done.add(accountService.create(account.getExternalSystemId(), deserialized));
		}
		for (int i = 0; i < modifiedAccounts.length(); i++) {
			JsUserAccountInfo account = modifiedAccounts.get(i);
			UserAccount deserialized = new UserAccountGwtSerDer().deserialize(new JSONObject(account));
			done.add(accountService.update(account.getExternalSystemId(), deserialized));
		}
		for (int i = 0; i < deletedAccounts.length(); i++) {
			String account = deletedAccounts.get(i);
			done.add(accountService.delete(account));
		}

		CompletableFuture.allOf(done.toArray(new CompletableFuture[0])).thenRun(() -> {
			handler.success(null);
		}).exceptionally(e -> {
			handler.failure(e);
			return null;
		});
	}

	private String getIcon(String identifier, byte[] logo) {
		String b64 = new String(logo);
		String base64 = atob(b64);
		base64 = "data:image/png;base64," + base64;
		return base64;
	}

	native String atob(String b64)
	/*-{
    return atob(b64);
	}-*/;

}

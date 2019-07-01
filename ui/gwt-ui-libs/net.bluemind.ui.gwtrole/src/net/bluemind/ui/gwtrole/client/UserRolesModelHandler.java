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
package net.bluemind.ui.gwtrole.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.directory.api.gwt.js.JsBaseDirEntryAccountType;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.gwt.endpoint.GroupEndpointPromise;
import net.bluemind.group.api.gwt.endpoint.GroupSockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.role.api.IRolesPromise;
import net.bluemind.role.api.gwt.endpoint.RolesSockJsEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.gwt.endpoint.UserEndpointPromise;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;
import net.bluemind.user.api.gwt.endpoint.UserSockJsEndpoint;
import net.bluemind.user.api.gwt.js.JsUser;

public class UserRolesModelHandler extends RolesModelHandler {

	public static final String TYPE = "bm.role.UserRolesModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler instance) {
				return new UserRolesModelHandler();
			}
		});
	}

	@Override
	public void load(final JavaScriptObject model, final AsyncHandler<Void> handler) {

		JsMapStringJsObject map = model.cast();
		JsUser u = map.get("user").cast();

		final RolesModel m = model.cast();
		m.setReadOnly(u.getAccountType() == JsBaseDirEntryAccountType.SIMPLE());

		IRolesPromise rolesEp = new RolesSockJsEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		CompletableFuture<Object> rFuture = rolesEp.getRoles().thenApply(value -> {
			m.setNativeRoles(value);
			return null;
		});

		CompletableFuture<Object> rcFuture = rolesEp.getRolesCategories().thenApply(value -> {
			m.setNativeCategories(value);
			return null;
		});

		JsMapStringJsObject values = model.cast();
		String domainUid = values.getString("domainUid");

		IUserPromise uep = new UserEndpointPromise(new UserSockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid));

		CompletableFuture<Void> rLoad = uep.getRoles(values.getString("userId")).thenAccept(value -> {
			JsArrayString roles = JsArrayString.createArray().cast();
			for (String v : value) {
				roles.push(v);
			}
			m.setRoles(roles);

		});
		CompletableFuture<Void> inheritedRoles = null;

		if (u.getAccountType() == JsBaseDirEntryAccountType.FULL()) {
			inheritedRoles = uep.memberOfGroups(values.getString("userId")).thenCompose(value -> {

				List<CompletableFuture<Set<String>>> rolesLoad = new ArrayList<>();

				for (String uid : value) {
					IGroupPromise gep = new GroupEndpointPromise(
							new GroupSockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid));
					CompletableFuture<Set<String>> roles = gep.getRoles(uid);
					rolesLoad.add(roles);
				}

				return CompletableFuture.allOf(rolesLoad.toArray(new CompletableFuture[0])).thenApply((v) -> {
					return rolesLoad.stream().map(CompletableFuture::join).reduce(new HashSet<>(), (l, r) -> {
						l.addAll(r);
						return l;
					});
				});
			}).thenAccept(value -> {
				JsArrayString roles = JsArrayString.createArray(value.size()).cast();
				for (String r : value) {
					roles.push(r);
				}
				values.put("inherited-roles", roles);
			});
		} else {
			inheritedRoles = uep.getResolvedRoles(values.getString("userId")).thenAccept(value -> {
				JsArrayString roles = JsArrayString.createArray(value.size()).cast();
				for (String r : value) {
					roles.push(r);
				}
				values.put("inherited-roles", roles);
			});
		}

		CompletableFuture.allOf(rFuture, rcFuture, rLoad, inheritedRoles).thenRun(() -> handler.success(null))
				.exceptionally(t -> {
					handler.failure(t);
					return null;
				});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		JsUser user = map.get("user").cast();
		if (user.getAccountType() == JsBaseDirEntryAccountType.SIMPLE()) {
			handler.success(null);
			return;
		}

		final RolesModel m = model.cast();
		JsArrayString roles = m.getRoles();
		Set<String> croles = new HashSet<>();
		for (int i = 0; i < roles.length(); i++) {
			croles.add(roles.get(i));
		}

		JsMapStringString values = model.cast();
		UserGwtEndpoint uep = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), values.get("domainUid"));

		uep.setRoles(values.get("userId"), croles, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				handler.success(null);
			}

		});
	}

}

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

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class GroupRolesModelHandler extends RolesModelHandler {

	public static final String TYPE = "bm.role.GroupRolesModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler instance) {
				return new GroupRolesModelHandler();
			}
		});
	}

	@Override
	public void load(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		final RolesModel m = model.cast();

		JsMapStringString values = model.cast();
		GroupGwtEndpoint uep = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), values.get("domainUid"));

		uep.getRoles(values.get("groupId"), new DefaultAsyncHandler<Set<String>>(handler) {

			@Override
			public void success(Set<String> value) {
				JsArrayString roles = JsArrayString.createArray().cast();
				for (String v : value) {
					roles.push(v);
				}
				m.setRoles(roles);
				loadRolesAndCategories(model, handler);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final RolesModel m = model.cast();
		JsArrayString roles = m.getRoles();
		Set<String> croles = new HashSet<>();
		for (int i = 0; i < roles.length(); i++) {
			croles.add(roles.get(i));
		}

		JsMapStringString values = model.cast();
		GroupGwtEndpoint uep = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), values.get("domainUid"));

		uep.setRoles(values.get("groupId"), croles, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				handler.success(null);
			}

		});
	}

}

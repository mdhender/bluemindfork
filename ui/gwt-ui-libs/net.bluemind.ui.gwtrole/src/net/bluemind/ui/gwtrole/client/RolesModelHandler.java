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

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.role.api.IRolesPromise;
import net.bluemind.role.api.gwt.endpoint.RolesSockJsEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public abstract class RolesModelHandler implements IGwtModelHandler {

	public void loadRolesAndCategories(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final RolesModel m = model.cast();
		final IRolesPromise ep = new RolesSockJsEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();

		CompletableFuture
				.allOf(
						// load roles and categories
						ep.getRolesCategories().thenAccept(value -> m.setNativeCategories(value)),
						// load roles
						ep.getRoles().thenAccept(value -> m.setNativeRoles(value)))
				.thenAccept(v -> handler.success(null)) //
				.exceptionally(t -> {
					handler.failure(t);
					return null;
				});
	}

}

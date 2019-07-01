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
package net.bluemind.ui.adminconsole.base.orgunit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.directory.api.gwt.js.JsOrgUnitPath;
import net.bluemind.directory.api.gwt.serder.OrgUnitPathGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class OrgUnitsAdministratorModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.role.OrgUnitsAdministratorModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler instance) {
				return new OrgUnitsAdministratorModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		String entryUid = map.getString("entryUid");
		String domainUid = map.getString("domainUid");

		OrgUnitsAdministratorModel ouModel = GWT.create(OrgUnitsAdministratorModel.class);
		ouModel.set(model);

		IOrgUnitsPromise orgUnits = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		orgUnits.listByAdministrator(entryUid, Collections.emptyList()).thenAccept(res -> {

			ouModel.orgUnits = null;
			List<OrgUnitAdministratorModel> list = new ArrayList<>();
			CompletableFuture<?>[] fp = res.stream().map(o -> {
				JsOrgUnitPath p = new OrgUnitPathGwtSerDer().serialize(o).isObject().getJavaScriptObject().cast();
				OrgUnitAdministratorModel ouRoles = GWT.create(OrgUnitAdministratorModel.class);
				ouRoles.modified = false;
				ouRoles.roles = new String[0];
				ouRoles.orgUnit = p;
				CompletableFuture<OrgUnitAdministratorModel> f = orgUnits.getAdministratorRoles(p.getUid(), entryUid, Collections.emptyList())
						.thenApply(roles -> {
							ouRoles.roles = roles.toArray(new String[0]);
							return ouRoles;
						});

				list.add(ouRoles);
				return f;
			}).toArray(CompletableFuture[]::new);

			CompletableFuture.allOf(fp).thenAccept(v -> {
				ouModel.orgUnits = list.toArray(new OrgUnitAdministratorModel[0]);
				handler.success(null);
			});

		}).exceptionally(e -> {
			handler.failure(e);
			return null;
		});

	}

	@Override
	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		String entryUid = map.getString("entryUid");
		String domainUid = map.getString("domainUid");
		OrgUnitsAdministratorModel ouModel = map.getObject("delegationModel");

		IOrgUnitsPromise orgUnits = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		List<CompletableFuture<Void>> res = new ArrayList<>();
		for (OrgUnitAdministratorModel ou : ouModel.orgUnits) {
			CompletableFuture<Void> p = orgUnits.setAdministratorRoles(ou.orgUnit.getUid(), entryUid,
					new HashSet<>(Arrays.asList(ou.roles)));
			res.add(p);
		}
		CompletableFuture.allOf(res.toArray(new CompletableFuture[0])).thenAccept(v -> {
			handler.success(null);
		}).exceptionally(t -> {
			handler.failure(t);
			return null;
		});

	}

}

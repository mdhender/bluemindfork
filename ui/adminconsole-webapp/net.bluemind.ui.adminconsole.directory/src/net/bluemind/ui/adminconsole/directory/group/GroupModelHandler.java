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
package net.bluemind.ui.adminconsole.directory.group;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.group.api.gwt.js.JsGroup;
import net.bluemind.group.api.gwt.serder.GroupGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class GroupModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.GroupModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new GroupModelHandler();
			}
		});
		GWT.log("bm.ac.GroupModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("groupId");
		String domainUid = map.getString("domainUid");
		IGroupPromise groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		CompletableFuture<Void> groupLoad = groups.getComplete(s).thenAccept(value -> {
			JsGroup group = new GroupGwtSerDer().serialize(value.value).isObject().getJavaScriptObject().cast();
			map.put("group", group);
			map.put("dirItem", new ItemValueGwtSerDer<>(new GroupGwtSerDer()).serialize(value).isObject()
					.getJavaScriptObject().cast());
		});

		CompletableFuture.allOf(groupLoad).thenRun(() -> handler.success(null)).exceptionally(t -> {
			handler.failure(t);
			return null;
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("groupId");
		String domainUid = map.getString("domainUid");
		IGroupPromise groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
		JsItemValue<JsGroup> itemGroup = map.get("dirItem").cast();
		JsGroup group = map.get("group").cast();

		CompletableFuture<Void> updateExtId = null;
		String extId = map.getString("updateExtId");
		if ((extId != itemGroup.getExternalId()) || (extId != null && !extId.equals(itemGroup.getExternalId()))) {
			updateExtId = groups.setExtId(s, extId);
		} else {
			updateExtId = CompletableFuture.completedFuture(null);
		}

		updateExtId.thenCompose(v -> {
			return groups.update(s, new GroupGwtSerDer().deserialize(new JSONObject(group)));
		}).thenAccept(v -> {
			handler.success(null);
		}).exceptionally(e -> {
			handler.failure(e);
			return null;
		});
	}
}

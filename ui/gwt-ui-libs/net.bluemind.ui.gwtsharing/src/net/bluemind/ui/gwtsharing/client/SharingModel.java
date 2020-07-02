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
package net.bluemind.ui.gwtsharing.client;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONArray;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.gwt.js.JsAccessControlEntry;
import net.bluemind.core.container.model.acl.gwt.serder.AccessControlEntryGwtSerDer;
import net.bluemind.directory.api.DirEntry;

public class SharingModel extends JavaScriptObject {

	protected SharingModel() {
	}

	public static void init(JavaScriptObject model, String modelId) {
		JsMapStringJsObject map = model.cast();
		if (map.get(modelId) == null) {
			map.put(modelId, JavaScriptObject.createObject());
		}
	}

	public static void populate(JavaScriptObject mainModel, String modelId, List<AccessControlEntry> acl) {

		JsArray<JsAccessControlEntry> jsAcl = null;
		if (acl != null) {
			jsAcl = new GwtSerDerUtils.ListSerDer<>(new AccessControlEntryGwtSerDer()).serialize(acl).isArray()
					.getJavaScriptObject().cast();
		}
		populate(mainModel, modelId, jsAcl);
	}

	public static void populate(JavaScriptObject mainModel, String modelId, DirEntry owner) {
		JsMapStringJsObject model = mainModel.<JsMapStringJsObject>cast().get(modelId).cast();
		model.putString("datalocation", owner.dataLocation);
	}

	public static void populate(JavaScriptObject mainModel, String modelId, JsArray<JsAccessControlEntry> jsAcl) {
		JsMapStringJsObject model = mainModel.<JsMapStringJsObject>cast().get(modelId).cast();
		if (model != null) {
			model.put("acl", jsAcl);
		}
	}

	public static SharingModel get(JavaScriptObject mainModel, String modelId) {
		JavaScriptObject object = mainModel.<JsMapStringJsObject>cast().get(modelId);
		if (object == null) {
			return null;
		} else {
			return object.cast();
		}
	}

	public final List<AccessControlEntry> getAcl() {
		JsArray<JsAccessControlEntry> acl = this.<JsMapStringJsObject>cast().get("acl").cast();
		return new GwtSerDerUtils.ListSerDer<>(new AccessControlEntryGwtSerDer()).deserialize(new JSONArray(acl));
	}

	public final JsArray<JsAccessControlEntry> getJsAcl() {
		return this.<JsMapStringJsObject>cast().get("acl").cast();
	}

	public final String getDataLocation() {
		return this.<JsMapStringJsObject>cast().getString("datalocation");
	}
}

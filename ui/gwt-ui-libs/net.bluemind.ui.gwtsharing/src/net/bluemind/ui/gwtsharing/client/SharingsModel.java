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

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.gwt.js.JsAccessControlEntry;
import net.bluemind.directory.api.DirEntry;

public final class SharingsModel extends JavaScriptObject {

	protected SharingsModel() {
	}

	public void populate(ContainerDescriptor descriptor, List<AccessControlEntry> entries) {
		JsMapStringJsObject map = this.cast();
		SharingModel.init(map, descriptor.uid);
		SharingModel.populate(map, descriptor.uid, entries);
	}
	
	public void populate(ContainerDescriptor descriptor, DirEntry owner) {
		JsMapStringJsObject map = this.cast();
		SharingModel.init(map, descriptor.uid);
		SharingModel.populate(map, descriptor.uid, owner);
	}
	
	public String[] getContainers() {
		JsMapStringJsObject map = this.cast();
		return map.keySet();
	}

	public final List<AccessControlEntry> getAcl(String containerId) {
		JsMapStringJsObject map = this.cast();
		return SharingModel.get(map, containerId).getAcl();
	}

	public JsArray<JsAccessControlEntry> getJsAcl(String uid) {
		return SharingModel.get(this, uid).getJsAcl();
	}

	public final String getDataLocation(String containerId) {
		JsMapStringJsObject map = this.cast();
		return SharingModel.get(map, containerId).getDataLocation();
	}
	
	public void setJsAcl(String uid, JsArray<JsAccessControlEntry> jsArray) {
		SharingModel.populate(this, uid, jsArray);
	}

	public static SharingsModel get(JavaScriptObject model, String modelId) {
		JsMapStringJsObject map = model.cast();
		return map.get(modelId).cast();
	}

	public static SharingsModel init(JavaScriptObject model, String modelId) {
		JsMapStringJsObject map = model.cast();
		map.put(modelId, JavaScriptObject.createObject());

		return map.get(modelId).cast();
	}

	public void removeData(String uid) {
		JsMapStringJsObject map = this.cast();
		map.remove(uid);
	}


}

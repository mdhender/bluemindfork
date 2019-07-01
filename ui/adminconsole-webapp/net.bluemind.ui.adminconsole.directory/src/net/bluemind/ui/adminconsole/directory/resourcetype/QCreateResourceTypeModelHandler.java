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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptorProperty;
import net.bluemind.resource.api.type.gwt.serder.ResourceTypeDescriptorGwtSerDer;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateResourceTypeModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateResourceTypeModelHandler";

	private QCreateResourceTypeModelHandler() {
	}

	public void save(JavaScriptObject model, AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		JsItemValue<JsDomain> domain = map.get("domain").cast();
		JsResourceTypeDescriptor rt = map.get("ResourceType").cast();
		rt.setProperties(JsArray.createArray().<JsArray<JsResourceTypeDescriptorProperty>> cast());

		ResourceTypesGwtEndpoint ResourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(),
				domain.getUid());
		String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("id", uid);

		ResourceTypes.create(uid, new ResourceTypeDescriptorGwtSerDer().deserialize(new JSONObject(rt)), handler);

	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateResourceTypeModel");
		JsMapStringJsObject map = model.cast();
		JsResourceTypeDescriptor rt = JsResourceTypeDescriptor.create();
		rt.setProperties(JsArray.createArray().<JsArray<JsResourceTypeDescriptorProperty>> cast());
		map.put("ResourceType", JsResourceTypeDescriptor.create());
		JSONValue jsonValue = new ItemValueGwtSerDer<>(new DomainGwtSerDer())
				.serialize(DomainsHolder.get().getSelectedDomain());
		map.put("domain", ((JSONObject) jsonValue).getJavaScriptObject());
		map.putString("domainUid", DomainsHolder.get().getSelectedDomain().uid);
		handler.success(null);
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateResourceTypeModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateResourceTypeModelHandler registred");
	}

}

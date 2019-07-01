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
import com.google.gwt.core.shared.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.resource.api.type.gwt.js.JsResourceType;
import net.bluemind.resource.api.type.gwt.js.JsResourceTypeDescriptor;
import net.bluemind.resource.api.type.gwt.serder.ResourceTypeDescriptorGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;

public class ResourceTypeModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.ResourceTypeModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new ResourceTypeModelHandler();
			}
		});
		GWT.log("bm.ac.ResourceTypeModelHandler registred");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("resourceTypeId");
		String domainUid = map.getString("domainUid");
		ResourceTypesGwtEndpoint ResourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		ResourceTypes.get(s, new AsyncHandler<ResourceTypeDescriptor>() {

			@Override
			public void success(ResourceTypeDescriptor value) {
				JsResourceType ResourceType = new ResourceTypeDescriptorGwtSerDer().serialize(value).isObject()
						.getJavaScriptObject().cast();
				map.put("resourceType", ResourceType);
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String s = map.getString("resourceTypeId");
		final String domainUid = map.getString("domainUid");
		ResourceTypesGwtEndpoint ResourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		final String icon = map.getString("resourceTypeIcon");
		JsResourceTypeDescriptor rt = map.get("resourceType").cast();

		ResourceTypes.update(s,
				new ResourceTypeDescriptorGwtSerDer().deserialize(new JSONObject(rt.<JavaScriptObject> cast())),
				new AsyncHandler<Void>() {

					@Override
					public void success(Void v) {
						if (icon == null) {
							handler.success(null);
						} else {
							doSetImage(domainUid, s, icon, handler);
						}
					}

					@Override
					public void failure(Throwable e) {
						handler.failure(e);
					}
				});

	}

	protected void doSetImage(String domainUid, String s, String icon, final AsyncHandler<Void> handler) {
		String path = "resourcetype/seticon";
		path = path + "?domainUid=" + domainUid + "&rtId=" + s + "&iconId=" + icon;
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		builder.setHeader("X-BM-ApiKey", Ajax.TOKEN.getSessionId());

		builder.setCallback(new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				handler.success(null);
			}

			@Override
			public void onError(Request request, Throwable exception) {
				handler.failure(exception);
			}
		});

		try {
			builder.send();
		} catch (RequestException e) {
			handler.failure(e);
		}
	}

}

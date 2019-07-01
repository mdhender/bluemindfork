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
package net.bluemind.ui.adminconsole.directory.resource;

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
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptor;
import net.bluemind.resource.api.gwt.serder.ResourceDescriptorGwtSerDer;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.gwt.endpoint.ResourceTypesGwtEndpoint;
import net.bluemind.resource.api.type.gwt.js.JsResourceType;
import net.bluemind.resource.api.type.gwt.serder.ResourceTypeDescriptorGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;

public class ResourceModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.ResourceModelHandler";

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("resourceId");
		String domainUid = map.getString("domainUid");
		ResourcesGwtEndpoint resources = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		resources.get(s, new AsyncHandler<ResourceDescriptor>() {

			@Override
			public void success(ResourceDescriptor value) {
				JsResourceDescriptor resource = new ResourceDescriptorGwtSerDer().serialize(value).isObject()
						.getJavaScriptObject().cast();
				map.put("resource", resource);

				loadResourceType(resource, map, handler);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}
		});
	}

	protected void loadResourceType(JsResourceDescriptor resource, final JsMapStringJsObject map,
			final AsyncHandler<Void> handler) {
		final String domainUid = map.getString("domainUid");
		ResourceTypesGwtEndpoint ResourceTypes = new ResourceTypesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		ResourceTypes.get(resource.getTypeIdentifier(), new DefaultAsyncHandler<ResourceTypeDescriptor>(handler) {

			@Override
			public void success(ResourceTypeDescriptor value) {
				JsResourceType ResourceType = new ResourceTypeDescriptorGwtSerDer().serialize(value).isObject()
						.getJavaScriptObject().cast();
				map.put("resourceType", ResourceType);
				handler.success(null);
			}

		});
	}

	@Override
	public void save(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String s = map.getString("resourceId");
		final String domainUid = map.getString("domainUid");
		JsResourceDescriptor desc = map.get("resource").cast();
		final String icon = map.getString("resourceIcon");

		ResourcesGwtEndpoint resources = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		resources.update(s, new ResourceDescriptorGwtSerDer().deserialize(new JSONObject(desc)),
				new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
						saveIcon(icon, domainUid, s, handler);
					}

				});

	}

	private void saveIcon(final String icon, final String domainUid, final String s, final AsyncHandler<Void> handler) {
		if (icon == null) {
			handler.success(null);
		} else {
			doSetImage(domainUid, s, icon, handler);
		}

	}

	protected void doSetImage(String domainUid, String s, String icon, final AsyncHandler<Void> handler) {
		String path = "resource/seticon";
		path = path + "?domainUid=" + domainUid + "&resourceId=" + s + "&iconId=" + icon;
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		builder.setHeader("X-BM-ApiKey", Ajax.TOKEN.getSessionId());

		builder.setCallback(new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				if (response.getStatusCode() >= 400) {
					handler.failure(new Exception("error : " + response.getStatusText()));
				} else {
					handler.success(null);
				}
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

	public static void registerType() {
		GwtModelHandler.register("bm.ac.ResourceModelHandler",
				new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

					@Override
					public IGwtModelHandler create(ModelHandler modelHandler) {
						return new ResourceModelHandler();
					}
				});
		GWT.log("bm.ac.ResourceModelHandler registred");
	}
}

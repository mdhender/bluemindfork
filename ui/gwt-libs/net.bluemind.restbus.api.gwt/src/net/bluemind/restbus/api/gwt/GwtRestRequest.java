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
package net.bluemind.restbus.api.gwt;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import net.bluemind.core.commons.gwt.JsMapStringString;

public final class GwtRestRequest extends JavaScriptObject {

	protected GwtRestRequest() {
	}

	public native void setPath(String path)
	/*-{
    this['path'] = path;
	}-*/;

	public native String getPath()
	/*-{
    return this['path'];
	}-*/;

	public native void setMethod(String method)
	/*-{
    this['method'] = method;
	}-*/;

	public native String getMethod()
	/*-{
    return this['method'];
	}-*/;

	public native void setHeaders(JsMapStringString headers)
	/*-{
    this['headers'] = headers;
	}-*/;

	public native JsMapStringString getHeaders()
	/*-{
    return this['headers'];
	}-*/;

	public native void setParams(JsMapStringString params)
	/*-{
    this['params'] = params
	}-*/;

	public native JsMapStringString getParams()
	/*-{
    return this['params'];
	}-*/;

	public native void setBody(JavaScriptObject body)
	/*-{
    if (body) {
      this['body'] = body;
    } else {
      this['body'] = null;
    }
	}-*/;

	public static GwtRestRequest create(String apiKey, String method, String path, Map<String, String> params,
			JavaScriptObject body) {
		JSONObject r = new JSONObject();
		r.put("path", new JSONString(path));
		r.put("method", new JSONString(method));

		JSONObject headers = new JSONObject();

		headers.put("X-BM-ApiKey", new JSONString(apiKey));
		r.put("headers", headers);

		JSONObject oparams = params != null ? new JSONObject(JsMapStringString.create(params)) : new JSONObject();
		r.put("params", oparams);

		GwtRestRequest request = r.getJavaScriptObject().cast();
		request.setBody(body);
		return request;
	}

	public static GwtRestRequest create(String apiKey, String method, String path, JavaScriptObject body) {
		return create(apiKey, method, path, null, body);
	}

}

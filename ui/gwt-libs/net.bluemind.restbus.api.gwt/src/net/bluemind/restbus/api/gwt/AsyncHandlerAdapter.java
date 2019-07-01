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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.restbus.api.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public abstract class AsyncHandlerAdapter<T> implements AsyncHandler<GwtRestResponse> {

	private AsyncHandler<T> handler;

	public AsyncHandlerAdapter(AsyncHandler<T> handler) {
		this.handler = handler;
	}

	@Override
	public void success(GwtRestResponse response) {
		int st = response.getStatusCode();
		if (st == 201 || st == 204) {
			handler.success(null);
		} else if (st >= 400 && st < 500) {
			// 4xx
			try {
				JSONObject jsonValue = new JSONObject(response.getBody());

				if ("ServerFault".equals(jsonValue.get("errorType").isString().stringValue())) {
					handler.failure(new ServerFault(jsonValue.get("message").isString().stringValue(),
							ErrorCode.valueOf(jsonValue.get("errorCode").isString().stringValue())));
					return;
				}

			} catch (Exception e) {

			}

			handler.failure(new ServerFault("error " + st));
		} else if (st >= 500) {
			try {
				JSONObject jsonValue = new JSONObject(response.getBody());

				if ("ServerFault".equals(jsonValue.get("errorType").isString().stringValue())) {
					handler.failure(new ServerFault(jsonValue.get("message").isString().stringValue(),
							ErrorCode.valueOf(jsonValue.get("errorCode").isString().stringValue())));
					return;
				}

			} catch (Exception e) {

			}
			// 5xx
			handler.failure(new ServerFault("error " + st));
		} else {
			JavaScriptObject content = response.getBody();
			JSONValue jsonValue = createObject(content);
			T r = handleResponse(jsonValue);
			handler.success(r);
		}
	}

	abstract protected T handleResponse(JSONValue content);

	private static native JSONValue createObject(Object o) /*-{
    if (!o) {
      return @com.google.gwt.json.client.JSONNull::getInstance()();
    }
    var v = o.valueOf ? o.valueOf() : o;
    if (v !== o) {
      // It was a primitive wrapper, unwrap it and try again.
      var func = @com.google.gwt.json.client.JSONParser::typeMap[typeof v];
      return func ? func(v)
          : @com.google.gwt.json.client.JSONParser::throwUnknownTypeException(Ljava/lang/String;)(typeof v);
    } else if (o instanceof Array || o instanceof $wnd.Array) {
      // Looks like an Array; wrap as JSONArray.
      // NOTE: this test can fail for objects coming from a different window,
      // but we know of no reliable tests to determine if something is an Array
      // in all cases.
      return @com.google.gwt.json.client.JSONArray::new(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
    } else {
      // This is a basic JavaScript object; wrap as JSONObject.
      // Subobjects will be created on demand.
      return @com.google.gwt.json.client.JSONObject::new(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
    }
	}-*/;

	@Override
	public void failure(Throwable e) {
		handler.failure(e);
	}

}

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
package net.bluemind.core.commons.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public abstract class EndpointRequestCallback<T> implements RequestCallback {

	private AsyncHandler<T> handler;

	public EndpointRequestCallback(AsyncHandler<T> handler) {
		this.handler = handler;
	}

	@Override
	public void onResponseReceived(Request request, Response response) {
		int st = response.getStatusCode();
		if (st == 201 || st == 204) {
			handler.success(null);
		} else if (st >= 400 && st < 500) {
			// 4xx
			try {
				JSONObject jsonValue = JSONParser.parseStrict(response.getText()).isObject();

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
				JSONObject jsonValue = JSONParser.parseStrict(response.getText()).isObject();

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
			String content = response.getText();
			if (content == null || content.length() == 0) {
				handler.success(null);
			} else {
				JSONValue jsonValue = JSONParser.parseStrict(response.getText());

				T resp = null;
				try {
					resp = handleResponse(jsonValue);
				} catch (Exception e) {
					handler.failure(e);
					return;
				}
				handler.success(resp);

			}
		}
	}

	protected abstract T handleResponse(JSONValue jsonValue);

	@Override
	public void onError(Request request, Throwable exception) {
		handler.failure(exception);
	}

}

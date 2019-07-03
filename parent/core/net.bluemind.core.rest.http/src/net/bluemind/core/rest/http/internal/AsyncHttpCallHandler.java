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
package net.bluemind.core.rest.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.RequestBuilder;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public class AsyncHttpCallHandler implements IRestCallHandler {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AsyncHttpCallHandler.class);

	private final String baseUri;
	private final AsyncHttpClient asyncHttpClient;
	private static final byte[] EMPTY_BODY = new byte[0];

	public AsyncHttpCallHandler(AsyncHttpClient asyncHttpClient, String baseUri) {
		this.asyncHttpClient = asyncHttpClient;
		this.baseUri = baseUri;
	}

	@Override
	public void call(RestRequest request, final AsyncHandler<RestResponse> responseHandler) {

		RequestBuilder requestBuilder = new RequestBuilder();
		requestBuilder.setMethod(request.method);
		requestBuilder.setBodyEncoding("utf-8");

		String path = request.path;
		requestBuilder.setUrl(baseUri + path);

		request.headers.forEachEntries(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));

		for (String val : request.remoteAddresses) {
			requestBuilder.addHeader("X-Forwarded-For", val);
		}

		if (request.origin != null) {
			requestBuilder.addHeader("X-BM-Origin", request.origin);
		}

		request.params.forEachEntries(entry -> requestBuilder.addQueryParam(entry.getKey(), entry.getValue()));

		if (request.body != null) {
			requestBuilder.setBody(request.body.getBytes());
		} else if (request.bodyStream != null) {
			requestBuilder.setBody(new BodyGeneratorStream(request.bodyStream));
		} else {
			requestBuilder.setBody(EMPTY_BODY);
		}

		logger.debug("execute query {}", requestBuilder);
		asyncHttpClient.prepareRequest(requestBuilder.build()).execute(new AsyncCompletionHandler(responseHandler));
	}

}

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

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;

public class AsyncHttpCallHandler implements IRestCallHandler {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AsyncHttpCallHandler.class);

	private final Uri baseUri;
	private final AsyncHttpClient asyncHttpClient;
	private static final byte[] EMPTY_BODY = new byte[0];
	private final PathPrep prep;

	public AsyncHttpCallHandler(AsyncHttpClient asyncHttpClient, Uri baseUri) {
		this.asyncHttpClient = asyncHttpClient;
		this.baseUri = baseUri;
		this.prep = baseUri.getPath().isEmpty() ? p -> p : p -> baseUri.getPath() + p;

	}

	private interface PathPrep {
		String apply(String r);
	}

	@Override
	public void call(RestRequest request, final AsyncHandler<RestResponse> responseHandler) {

		RequestBuilder requestBuilder = new RequestBuilder();
		requestBuilder.setMethod(request.method.name());
		Uri parsedUri = new Uri(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(),
				prep.apply(request.path), null, null);
		requestBuilder.setUri(parsedUri);

		request.headers.forEach(entry -> requestBuilder.addHeader(entry.getKey(), entry.getValue()));

		requestBuilder.addHeader("X-Forwarded-For", request.remoteAddresses);

		if (request.origin != null) {
			requestBuilder.addHeader("X-BM-Origin", request.origin);
		}

		request.params.forEach(entry -> requestBuilder.addQueryParam(entry.getKey(), entry.getValue()));

		if (request.body != null) {
			requestBuilder.setBody(request.body.getBytes());
		} else if (request.bodyStream != null) {
			requestBuilder.setBody(new BodyGeneratorStream(request.bodyStream));
		} else {
			requestBuilder.setBody(EMPTY_BODY);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("execute query {}", requestBuilder);
		}
		asyncHttpClient.prepareRequest(requestBuilder.build()).execute(new AsyncCompletionHandler(responseHandler));
	}

}

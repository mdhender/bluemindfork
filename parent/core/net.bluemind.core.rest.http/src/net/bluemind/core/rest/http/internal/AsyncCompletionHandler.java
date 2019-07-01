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

import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.streams.ReadStream;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Response;

import io.netty.buffer.Unpooled;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.RestResponse;

public class AsyncCompletionHandler extends AsyncCompletionHandlerBase {
	private static final Logger logger = LoggerFactory.getLogger(AsyncCompletionHandler.class);
	private AsyncHandler<RestResponse> responseHandler;
	private boolean chunked;
	private BufferedStream bufferedStream;

	public AsyncCompletionHandler(AsyncHandler<RestResponse> responseHandler) {
		this.responseHandler = responseHandler;
	}

	@Override
	public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
		chunked = "chunked".equals(headers.getHeaders().getFirstValue("Transfer-Encoding"));
		if (chunked) {
			logger.debug("chuncked response");
			bufferedStream = new BufferedStream();
			responseHandler.success(RestResponse.stream((ReadStream<?>) bufferedStream));
		}
		return super.onHeadersReceived(headers);
	}

	@Override
	public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
		if (chunked) {
			Buffer chunk = new Buffer(Unpooled.wrappedBuffer(content.getBodyByteBuffer()));
			logger.debug("recieve chunk of chuncked response {}", chunk);
			bufferedStream.write(chunk);
			return STATE.CONTINUE;
		} else {
			return super.onBodyPartReceived(content);
		}
	}

	@Override
	public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
		logger.debug("stat receive {}", status);
		return super.onStatusReceived(status);
	}

	@Override
	public Response onCompleted(Response response) throws Exception {
		try {
			if (chunked) {

				logger.debug("end of sream");
				bufferedStream.end();
			} else {
				logger.debug("normal response  {}", response.getStatusCode());
				RestResponse resp = new RestResponse(response.getStatusCode());
				CaseInsensitiveMultiMap h = new CaseInsensitiveMultiMap();
				for (Entry<String, List<String>> he : response.getHeaders().entrySet()) {
					h.add(he.getKey(), he.getValue());
				}
				resp.headers = h;

				byte[] responseBody = response.getResponseBodyAsBytes();
				if (responseBody != null && responseBody.length > 0) {
					resp.data = new Buffer(Unpooled.wrappedBuffer(responseBody));
				}

				responseHandler.success(resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	@Override
	public void onThrowable(Throwable t) {
		responseHandler.failure(t);
	}

}

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

import java.nio.ByteBuffer;
import java.util.Map.Entry;

import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.streams.ReadStream;
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
	public State onHeadersReceived(final HttpHeaders headers) throws Exception {

		chunked = "chunked".equals(headers.get("Transfer-Encoding"));
		if (chunked) {
			logger.debug("chuncked response");
			bufferedStream = new BufferedStream();
			responseHandler.success(RestResponse.stream((ReadStream<Buffer>) bufferedStream));
		}
		return super.onHeadersReceived(headers);
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
		if (chunked) {
			Buffer chunk = Buffer.buffer(content.getBodyPartBytes());
			logger.debug("recieve chunk of chuncked response {}", chunk);
			bufferedStream.write(chunk);
			return State.CONTINUE;
		} else {
			return super.onBodyPartReceived(content);
		}
	}

	@Override
	public State onStatusReceived(HttpResponseStatus status) throws Exception {
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

				CaseInsensitiveHeaders h = new CaseInsensitiveHeaders();
				for (Entry<String, String> he : response.getHeaders().entries()) {
					h.add(he.getKey(), he.getValue());
				}
				resp.headers = h;

				ByteBuffer responseBody = response.getResponseBodyAsByteBuffer();
				if (responseBody != null) {
					resp.data = Buffer.buffer(Unpooled.wrappedBuffer(responseBody));
				}

				responseHandler.success(resp);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return response;
	}

	@Override
	public void onThrowable(Throwable t) {
		responseHandler.failure(t);
	}

}

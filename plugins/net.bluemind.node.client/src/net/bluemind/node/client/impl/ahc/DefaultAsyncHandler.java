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
package net.bluemind.node.client.impl.ahc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

import net.bluemind.common.io.FileBackedOutputStream;

public abstract class DefaultAsyncHandler<T> implements AsyncHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncHandler.class);

	protected final FileBackedOutputStream body;
	protected int status;

	private HttpResponseHeaders headers;

	protected DefaultAsyncHandler(boolean bodyExpected) {
		if (bodyExpected) {
			this.body = new FileBackedOutputStream(65536, "node-client-async");
		} else {
			this.body = null;
		}
	}

	@Override
	public void onThrowable(Throwable t) {
		logger.warn("onThrowable: {} {}", t.getClass(), t.getMessage());
		if (body != null) {
			try {
				body.close();
				body.reset();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		if (body != null) {
			bodyPart.writeTo(body);
		}
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus)
			throws Exception {
		this.status = responseStatus.getStatusCode();
		logger.debug("status: {}", status);
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
		this.headers = headers;
		return STATE.CONTINUE;
	}

	@Override
	public T onCompleted() throws Exception {
		logger.debug("onCompleted");
		if (body != null) {
			body.close();
		}
		return getResult(status, headers, body);
	}

	protected abstract T getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body);

	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		return rb;
	}

}

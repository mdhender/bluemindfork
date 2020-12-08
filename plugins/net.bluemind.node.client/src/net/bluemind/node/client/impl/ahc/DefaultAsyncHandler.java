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
import java.util.Optional;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;

public abstract class DefaultAsyncHandler<T> implements AsyncHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncHandler.class);

	protected final FileBackedOutputStream body;
	private final Optional<String> stackInfos;
	protected int status;
	private HttpHeaders headers;

	protected DefaultAsyncHandler(boolean bodyExpected) {
		this(null, bodyExpected);
	}

	protected DefaultAsyncHandler(String stackInfos, boolean bodyExpected) {
		this.stackInfos = Optional.ofNullable(stackInfos);
		if (bodyExpected) {
			this.body = new FileBackedOutputStream(65536, "node-client-async");
		} else {
			this.body = null;
		}
	}

	@Override
	public void onThrowable(Throwable t) {
		if (logger.isWarnEnabled()) {
			logger.warn("onThrowable for '{}': {} {}", stackInfos.orElseGet(() -> getClass().getCanonicalName()),
					t.getClass(), t.getMessage());
		}
		if (body != null) {
			try {
				body.close();
				body.reset();
			} catch (IOException e) {
				// ok
			}
		}
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		if (body != null) {
			body.write(bodyPart.getBodyPartBytes());
		}
		return State.CONTINUE;
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		this.status = responseStatus.getStatusCode();
		logger.debug("status: {}", status);
		return State.CONTINUE;
	}

	@Override
	public State onHeadersReceived(HttpHeaders headers) throws Exception {
		this.headers = headers;
		return State.CONTINUE;
	}

	@Override
	public T onCompleted() throws Exception {
		logger.debug("onCompleted");
		if (body != null) {
			body.close();
		}
		return getResult(status, headers, body);
	}

	protected abstract T getResult(int status, HttpHeaders headers, FileBackedOutputStream body);

	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		return rb;
	}

}

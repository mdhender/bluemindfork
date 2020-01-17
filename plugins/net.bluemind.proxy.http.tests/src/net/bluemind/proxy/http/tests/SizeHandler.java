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
package net.bluemind.proxy.http.tests;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;

public class SizeHandler implements AsyncHandler<Long> {

	private static final Logger logger = LoggerFactory.getLogger(SizeHandler.class);
	private long size;

	public SizeHandler() {
		this.size = 0L;
	}

	@Override
	public void onThrowable(Throwable t) {
		logger.error(t.getMessage(), t);
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		int length = bodyPart.getBodyPartBytes().length;
		size += length;
		return State.CONTINUE;
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		logger.info("status received: " + responseStatus.getStatusCode());
		return State.CONTINUE;
	}

	@Override
	public State onHeadersReceived(HttpHeaders headers) throws Exception {
		return State.CONTINUE;
	}

	@Override
	public Long onCompleted() throws Exception {
		return size;
	}

}

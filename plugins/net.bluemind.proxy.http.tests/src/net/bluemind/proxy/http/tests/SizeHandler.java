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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

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
	public com.ning.http.client.AsyncHandler.STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		int length = bodyPart.getBodyPartBytes().length;
		size += length;
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus)
			throws Exception {
		logger.info("status received: " + responseStatus.getStatusCode());
		return STATE.CONTINUE;
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
		return STATE.CONTINUE;
	}

	@Override
	public Long onCompleted() throws Exception {
		return size;
	}

}

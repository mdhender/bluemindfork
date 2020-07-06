/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.calendar.helper.mail;

import java.util.Optional;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;

public class BudgetBasedDownloader implements AsyncHandler<Optional<byte[]>> {

	private static final Logger logger = LoggerFactory.getLogger(BudgetBasedDownloader.class);

	private ByteBuf target;
	private long budget;
	private boolean tooBig;

	public BudgetBasedDownloader(long budget) {
		this.budget = budget;
		this.target = Unpooled.buffer();
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		return State.CONTINUE;
	}

	@Override
	public State onHeadersReceived(HttpHeaders headers) throws Exception {
		Integer cl = headers.getInt(HttpHeaderNames.CONTENT_LENGTH);
		if (cl != null && cl > budget) {
			this.tooBig = true;
			return State.ABORT;
		}
		return State.CONTINUE;
	}

	@Override
	public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		target.writeBytes(bodyPart.getBodyByteBuffer());
		if (target.readableBytes() > budget) {
			this.tooBig = true;
			return State.ABORT;
		}
		return State.CONTINUE;
	}

	@Override
	public void onThrowable(Throwable t) {
		logger.error(t.getMessage(), t);
	}

	@Override
	public Optional<byte[]> onCompleted() throws Exception {
		return tooBig ? Optional.empty() : Optional.of(asBytes());
	}

	private byte[] asBytes() {
		byte[] tgt = new byte[target.readableBytes()];
		target.readBytes(tgt);
		return tgt;
	}

}

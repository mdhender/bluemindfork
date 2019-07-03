/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.vertx;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;

public class ImapProtocolListener<T> {
	private static final Logger logger = LoggerFactory.getLogger(ImapProtocolListener.class);

	public final CompletableFuture<T> future;

	public ImapProtocolListener(CompletableFuture<T> future) {
		this.future = future;
	}

	/**
	 * When we don't care about intermediate status responses
	 * 
	 * @return
	 */
	public static ImapProtocolListener<Void> noExpectations() {
		return new ImapProtocolListener<>(CompletableFuture.completedFuture(null));
	}

	public ImapProtocolListener() {
		this(new CompletableFuture<>());
	}

	/**
	 * Invoked on for status responses (lines starting with a "* "). If this
	 * response is followed by a literal, it is not included in the given buffer and
	 * a {@link #onBinary(ByteBuf)} call will follow.
	 * 
	 * @param s the status response
	 */
	public void onStatusResponse(ByteBuf s) {
		logger.info("S: {}", s);
	}

	/**
	 * This is called with the command completion line holding the response tag and
	 * the status message. Some commands return some data there (APPEND, UID COPY).
	 * 
	 * The tag is already removed from the received buffer, and the status too.
	 * 
	 * @param status
	 * 
	 * @param s
	 */
	public void onTaggedCompletion(Status status, ByteBuf s) {
		// most commands don't care, but APPEND returns the new UID in here
		if (logger.isDebugEnabled()) {
			logger.debug("tagged completion, status: {}, all: {}", status, s.toString(StandardCharsets.US_ASCII));
		}
	}

	public void onBinary(ByteBuf b) {
		if (logger.isDebugEnabled()) {
			logger.debug("S: {}byte(s)", b.readableBytes());
		}
	}
}
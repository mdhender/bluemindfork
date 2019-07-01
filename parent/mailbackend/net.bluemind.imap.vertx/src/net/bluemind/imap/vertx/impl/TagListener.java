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
package net.bluemind.imap.vertx.impl;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.imap.vertx.ImapProtocolListener;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.utils.BufUtils;

/**
 * For simple text based commands expecting 0 or more spurious responses and a
 * tagged response
 *
 * @param <T>
 */
public class TagListener<T> extends ImapProtocolListener<ImapResponseStatus<T>> {

	private static final Logger logger = LoggerFactory.getLogger(TagListener.class);
	private final ImapProtocolListener<T> delegate;
	private final ByteBuf tagNeedle;

	public TagListener(String tag, ImapProtocolListener<T> delegate) {
		this.tagNeedle = Unpooled.wrappedBuffer((tag + " ").getBytes());
		this.delegate = delegate;
	}

	public void onStatusResponse(ByteBuf b) {
		if (BufUtils.indexOf(b, tagNeedle) == 0) {
			int tagLen = tagNeedle.readableBytes();
			char afterTag = (char) b.getByte(tagLen);
			Status status = null;
			int statusLen = 3; // 'Ok '
			switch (afterTag) {
			case 'O':
				status = Status.Ok;
				break;
			case 'N':
				status = Status.No;
				break;
			default:
			case 'B':
				statusLen = 4;
				status = Status.Bad;
				break;
			}
			delegate.onTaggedCompletion(status, b.slice(tagLen + statusLen, b.readableBytes() - (tagLen + statusLen)));
			if (delegate.future.isDone()) {
				try {
					future.complete(new ImapResponseStatus<>(status, delegate.future.join()));
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				future.completeExceptionally(
						new Throwable("tag before expectations: " + b.toString(StandardCharsets.US_ASCII)));
			}

		} else {
			delegate.onStatusResponse(b);
		}

	}

	public void onBinary(ByteBuf b) {
		delegate.onBinary(b);
	}

}

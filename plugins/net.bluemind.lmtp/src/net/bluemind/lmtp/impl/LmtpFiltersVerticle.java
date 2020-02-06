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
package net.bluemind.lmtp.impl;

import java.io.OutputStream;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import net.bluemind.lmtp.backend.FilterException;
import net.bluemind.lmtp.backend.IMessageFilter;
import net.bluemind.lmtp.backend.LmtpReply;
import net.bluemind.lmtp.backend.PermissionDeniedException;
import net.bluemind.lmtp.impl.busmessages.MailMessage;
import net.bluemind.mime4j.common.Mime4JHelper;

public class LmtpFiltersVerticle extends AbstractVerticle {

	public static final String ADDR = "lmtp.filters";

	private static final Logger logger = LoggerFactory.getLogger(LmtpFiltersVerticle.class);

	private Handler<Message<MailMessage>> filtersHandler;
	private LmtpConfig config;

	private MessageConsumer<MailMessage> consumer;

	@Override
	public void start() {
		config = new LmtpConfig();
		filtersHandler = (Message<MailMessage> event) -> {

			try {
				MailMessage tm = applyFilters(event.body());
				event.reply(tm);
			} catch (PermissionDeniedException e) {
				// BM-8652
				logger.error("Permission denied, add X-BM-Discard header");
				RawField rf = new RawField("X-BM-Discard", e.toHeaderValue());
				UnstructuredField discard = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
				try {
					MailMessage mm = addHeader(event.body(), discard);
					event.reply(mm);
				} catch (FilterException e1) {
					logger.error("error during setting X-BM-Discard header", e);
					event.reply(event.body());
				}
			} catch (FilterException e) {
				logger.error("error during filtering message body", e);
				// BM-7152 add X-BM-Error header
				RawField rf = new RawField("X-BM-Error", e.getErrorCode());
				UnstructuredField error = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
				try {
					MailMessage mm = addHeader(event.body(), error);
					event.reply(mm);
				} catch (FilterException e1) {
					logger.error("error during setting X-BM-Error header", e);
					event.reply(event.body());

				}

			}

		};

		this.consumer = vertx.eventBus().consumer(ADDR, filtersHandler);
	}

	@Override
	public void stop() {
		consumer.unregister();
	}

	protected MailMessage applyFilters(MailMessage body) throws FilterException {

		if (config.getFilters().isEmpty()) {
			return body;
		}

		logger.debug("apply filters for mail id {}", body.getEnvelope().getId());
		org.apache.james.mime4j.dom.Message msg = parseMessage(body.getData());

		org.apache.james.mime4j.dom.Message filtered = msg;
		boolean modified = false;
		for (IMessageFilter filter : config.getFilters()) {
			try {
				org.apache.james.mime4j.dom.Message fresh = filter.filter(body.getEnvelope(), filtered,
						(long) body.getData().readableBytes());
				if (fresh != null) {
					modified = true;
					if (fresh != filtered) {
						// when a filter replaces the message instead of
						// updating it, dispose the previous one
						filtered.dispose();
					}
					filtered = fresh;
				}
			} catch (FilterException fe) {
				throw fe;
			}
		}

		// no body modification
		if (!modified) {
			// return original body
			filtered.dispose();
			return body;
		} else {
			logger.debug("mail body has been modified");
			ByteBuf data = writeFilteredMessage(filtered);
			filtered.dispose();
			return new MailMessage(body.getEnvelope(), data);
		}

	}

	private MailMessage addHeader(MailMessage mm, UnstructuredField f) throws FilterException {
		org.apache.james.mime4j.dom.Message msg = parseMessage(mm.getData());
		msg.getHeader().addField(f);
		ByteBuf data = writeFilteredMessage(msg);
		return new MailMessage(mm.getEnvelope(), data);

	}

	private ByteBuf writeFilteredMessage(org.apache.james.mime4j.dom.Message message) throws FilterException {
		ByteBuf result = Unpooled.buffer();
		try (org.apache.james.mime4j.dom.Message m = message; OutputStream out = new ByteBufOutputStream(result)) {
			MessageServiceFactoryImpl.newInstance().newMessageWriter().writeMessage(m, out);
			return result;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new FilterException(LmtpReply.TEMPORARY_FAILURE, "Internal error");
		}
	}

	private org.apache.james.mime4j.dom.Message parseMessage(ByteBuf buf) {

		org.apache.james.mime4j.dom.Message message = Mime4JHelper.parse(new ByteBufInputStream(buf.duplicate()));
		sanitizeEmail(message);
		return message;
	}

	private void sanitizeEmail(Entity message) {
		// Mime4j bug: https://issues.apache.org/jira/browse/MIME4J-214
		if (message.isMultipart()) {
			Header headers = message.getHeader();
			if (headers != null) {
				headers.removeFields("Content-Transfer-Encoding");
			}

			Multipart body = (Multipart) message.getBody();
			for (Entity part : body.getBodyParts())
				sanitizeEmail(part);
		}
	}
}

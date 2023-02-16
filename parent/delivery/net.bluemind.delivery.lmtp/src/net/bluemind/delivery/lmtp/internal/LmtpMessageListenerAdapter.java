/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.delivery.lmtp.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;

import net.bluemind.delivery.lmtp.MmapRewindStream;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class LmtpMessageListenerAdapter implements MessageHandlerFactory {

	private final LmtpListener delegate;
	private static final int DEFAULT_MESSAGE_SIZE_LIMIT = 10 * 1024 * 1024; // 10485760L

	/**
	 * Initializes this factory with a single listener.
	 *
	 * @param listener {@link LmtpListener}
	 */
	public LmtpMessageListenerAdapter(LmtpListener listener) {
		this.delegate = listener;
	}

	public MessageHandler create(MessageContext ctx) {
		return new Handler(ctx);
	}

	/**
	 * Needed by this class to track which listeners need delivery.
	 */
	static class Delivery {
		LmtpListener listener;

		public LmtpListener getListener() {
			return this.listener;
		}

		String recipient;

		public String getRecipient() {
			return this.recipient;
		}

		public Delivery(LmtpListener listener, String recipient) {
			this.listener = listener;
			this.recipient = recipient;
		}
	}

	/**
	 * Class which implements the actual handler interface.
	 */
	class Handler implements MessageHandler, ILmtpExtendedHandler {
		MessageContext ctx;
		String from;
		List<Delivery> deliveries = new ArrayList<>();

		public Handler(MessageContext ctx) {
			this.ctx = ctx;
		}

		public void from(String from) throws RejectException {
			this.from = from;
		}

		public void recipient(String recipient) throws RejectException {
			RecipientDeliveryStatus status = delegate.accept(this.from, recipient);
			if (status.accept().deliver()) {
				this.deliveries.add(new Delivery(delegate, recipient));
			} else {
				throw new RejectException(status.accept().code(),
						"<" + recipient + "> rejected, reason: " + status.reason());
			}
		}

		public void data(InputStream data) throws IOException {
			lmtpData(data);
		}

		public List<RecipientDeliveryStatus> lmtpData(InputStream data) throws IOException {
			Integer messageMaxSize = LocalSysconfCache.get().integerValue(SysConfKeys.message_size_limit.name());
			MmapRewindStream stream = new MmapRewindStream(data,
					messageMaxSize == null ? DEFAULT_MESSAGE_SIZE_LIMIT : messageMaxSize);

			List<RecipientDeliveryStatus> result = new ArrayList<>(deliveries.size());
			for (Delivery delivery : deliveries) {
				try {
					delivery.getListener().deliver(this.from, delivery.getRecipient(), stream.byteBufRewinded());
					result.add(RecipientAcceptance.ACCEPT.reason(null));
				} catch (Exception e) {
					result.add(RecipientAcceptance.TEMPORARY_REJECT.reason(e.getMessage()));
				}
			}
			return result;
		}

		public void done() {
		}
	}
}

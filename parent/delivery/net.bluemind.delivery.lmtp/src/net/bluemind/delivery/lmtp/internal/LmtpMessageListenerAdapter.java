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
import java.util.Collections;
import java.util.List;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.io.DeferredFileOutputStream;

import net.bluemind.utils.ByteSizeUnit;

public class LmtpMessageListenerAdapter implements MessageHandlerFactory {

	/**
	 * 5 megs by default. The server will buffer incoming messages to disk when they
	 * hit this limit in the DATA received.
	 */
	private static int DEFAULT_DATA_DEFERRED_SIZE = (int) ByteSizeUnit.MB.toBytes(5);

	private final LmtpListener delegate;
	private final int dataDeferredSize;

	/**
	 * Initializes this factory with a single listener.
	 *
	 * Default data deferred size is 5 megs.
	 */
	public LmtpMessageListenerAdapter(LmtpListener listener) {
		this(listener, DEFAULT_DATA_DEFERRED_SIZE);
	}

	/**
	 * Initializes this factory with the listeners.
	 * 
	 * @param dataDeferredSize The server will buffer incoming messages to disk when
	 *                         they hit this limit in the DATA received.
	 */
	public LmtpMessageListenerAdapter(LmtpListener listener, int dataDeferredSize) {
		this.delegate = listener;
		this.dataDeferredSize = dataDeferredSize;
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
			if (this.deliveries.size() == 1) {
				Delivery delivery = this.deliveries.get(0);
				try {
					delivery.getListener().deliver(this.from, delivery.getRecipient(), data);
					return Collections.singletonList(RecipientAcceptance.ACCEPT.reason(null));
				} catch (Exception e) {
					return Collections.singletonList(RecipientAcceptance.TEMPORARY_REJECT.reason(e.getMessage()));
				}
			} else {
				List<RecipientDeliveryStatus> result = new ArrayList<>(deliveries.size());
				try (DeferredFileOutputStream dfos = new DeferredFileOutputStream(dataDeferredSize)) {
					int value;
					while ((value = data.read()) >= 0) {
						dfos.write(value);
					}

					for (Delivery delivery : deliveries) {
						try {
							delivery.getListener().deliver(this.from, delivery.getRecipient(), dfos.getInputStream());
							result.add(RecipientAcceptance.ACCEPT.reason(null));
						} catch (Exception e) {
							result.add(RecipientAcceptance.TEMPORARY_REJECT.reason(e.getMessage()));
						}
					}
				}
				return result;
			}
		}

		public void done() {
		}
	}
}

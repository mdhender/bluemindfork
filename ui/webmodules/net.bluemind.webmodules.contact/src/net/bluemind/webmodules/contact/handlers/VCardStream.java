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
package net.bluemind.webmodules.contact.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.addressbook.api.IVCardServicePromise;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;

public class VCardStream implements ReadStream<VCardStream> {

	private static Logger logger = LoggerFactory.getLogger(VCardStream.class);

	private Handler<Buffer> dataHandler;
	private boolean closed;
	private boolean paused;
	private Handler<Void> endHandler;
	private boolean readInProgress = false;
	// key = container-uid, values=vcard uids
	private Map<String, List<String>> cards;
	private Iterator<String> serviceIterator;
	private Iterator<String> currentCardIterator;
	private IServiceProvider clientProvider;
	private IVCardServicePromise currentService;

	private Handler<Throwable> exceptionHandler;

	public VCardStream(IServiceProvider clientProvider, Map<String, List<String>> cards) {
		this.serviceIterator = cards.keySet().iterator();
		this.cards = cards;
		this.clientProvider = clientProvider;
	}

	@Override
	public VCardStream dataHandler(Handler<Buffer> handler) {
		check();
		this.dataHandler = handler;
		if (dataHandler != null && !paused && !closed) {
			doRead();
		}
		return this;
	}

	@Override
	public VCardStream pause() {
		check();
		readInProgress = false;
		paused = true;
		return this;
	}

	@Override
	public VCardStream resume() {
		check();
		if (paused && !closed) {
			paused = false;
			if (dataHandler != null) {
				doRead();
			}
		}
		return this;
	}

	@Override
	public VCardStream exceptionHandler(Handler<Throwable> handler) {
		check();
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public VCardStream endHandler(Handler<Void> handler) {
		check();
		this.endHandler = handler;
		return this;
	}

	private void check() {
	}

	private void doRead() {
		if (!readInProgress) {
			readInProgress = true;

			if (null == currentCardIterator || !currentCardIterator.hasNext()) {
				String serviceContainer = serviceIterator.next();
				currentService = ((VertxPromiseServiceProvider) clientProvider).instance(IVCardServicePromise.class,
						serviceContainer);
				currentCardIterator = cards.get(serviceContainer).iterator();
			}

			List<String> toRead = new ArrayList<>(1000);
			int length = 0;
			while (currentCardIterator.hasNext() && length < 1000) {
				toRead.add(currentCardIterator.next());
				length++;
			}

			CompletableFuture<String> exportCards = currentService.exportCards(toRead);
			exportCards.thenAccept(value -> {
				readInProgress = false;
				dataHandler.handle(new Buffer(value));
				if (!currentCardIterator.hasNext() && !serviceIterator.hasNext()) {
					handleEnd();
				} else {
					if (!paused && dataHandler != null) {
						doRead();
					}
				}

			});

			exportCards.exceptionally(e -> {
				handleException(e);
				readInProgress = false;
				return null;
			});

		}
	}

	private void handleEnd() {
		if (endHandler != null) {
			endHandler.handle(null);
		}
	}

	private void handleException(Throwable t) {
		if (exceptionHandler != null && t instanceof Exception) {
			exceptionHandler.handle(t);
		} else {
			logger.error("Unhandled exception", t);
		}
	}
}

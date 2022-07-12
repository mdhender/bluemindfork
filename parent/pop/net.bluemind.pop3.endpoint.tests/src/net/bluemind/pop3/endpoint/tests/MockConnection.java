/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint.tests;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.streams.WriteStream;
import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.Stat;

public class MockConnection implements MailboxConnection {
	private static final Logger logger = LoggerFactory.getLogger(MockConnection.class);

	private String login;

	public MockConnection(String login) {
		this.login = login;
	}

	@Override
	public void close() {
		logger.info("[{}] close", login);
	}

	@Override
	public Stat stat() {
		return new Stat(1, 1024L);
	}

	@Override
	public CompletableFuture<Void> list(WriteStream<ListItem> output) {
		CompletableFuture<Void> promise = new CompletableFuture<>();
		output.write(new ListItem(1, 1024), ar -> {
			output.end();
			if (ar.failed()) {
				promise.completeExceptionally(ar.cause());
			} else {
				promise.complete(null);
			}
		});
		return promise;
	}

}

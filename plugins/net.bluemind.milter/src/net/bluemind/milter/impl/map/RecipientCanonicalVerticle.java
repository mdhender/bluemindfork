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
package net.bluemind.milter.impl.map;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.milter.map.RecipientCanonical;

public class RecipientCanonicalVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(RecipientCanonicalVerticle.class);

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new RecipientCanonicalVerticle();
		}
	}

	private class RecipientCanonicalBuffer implements Handler<Buffer> {
		private final long IDLE_TIMEOUT = TimeUnit.HOURS.toMillis(1);

		private NetSocket event;
		private long timerId;
		private Collection<RecipientCanonical> recipientCanonical;

		public RecipientCanonicalBuffer(NetSocket event) {
			this.event = event;
			recipientCanonical = RecipientCanonicalRegistry.get();
		}

		public void setTimeout() {
			this.timerId = vertx.setTimer(IDLE_TIMEOUT, timerId -> event.close());
		}

		@Override
		public void handle(Buffer buf) {
			vertx.cancelTimer(timerId);

			String cmd = buf.toString();
			if (Strings.isNullOrEmpty(cmd) || !cmd.toLowerCase().startsWith("get ")) {
				sendResponse("500 Invalid command");
				return;
			}

			String val = cmd.substring("get ".length());
			if (Strings.isNullOrEmpty(val)) {
				sendResponse("500 Invalid value");
				return;
			}

			sendResponse(recipientCanonical.stream().map(rc -> rc.execute(val)).filter(Optional::isPresent)
					.map(Optional::get).findFirst().map(response -> "200 " + response).orElse("500 Nothing to do"));
		}

		private void sendResponse(String response) {
			event.write(response + "\n");
			setTimeout();
		}
	}

	@Override
	public void start() {
		NetServer server = vertx.createNetServer();
		server.connectHandler(new Handler<NetSocket>() {
			@Override
			public void handle(NetSocket event) {
				RecipientCanonicalBuffer ssrb = new RecipientCanonicalBuffer(event);
				ssrb.setTimeout();

				event.handler(RecordParser.newDelimited("\n", ssrb));
			}
		});

		logger.info("Recipient canonicalrewrite verticle listening on {}.", 25251);
		server.listen(25251, "127.0.0.1");
	}
}

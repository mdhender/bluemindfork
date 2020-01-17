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
package net.bluemind.ysnp.bmcore.internal;

import java.util.concurrent.TimeUnit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;

public class PostfixSenderVerticle extends AbstractVerticle {

	private class SmtpdSenderRestrictionsBuffer implements Handler<Buffer> {
		private final long IDLE_TIMEOUT = TimeUnit.HOURS.toMillis(1);

		private NetSocket event;
		private long timerId;

		public SmtpdSenderRestrictionsBuffer(NetSocket event) {
			this.event = event;
		}

		public void setTimeout() {
			this.timerId = vertx.setTimer(IDLE_TIMEOUT, timerId -> event.close());
		}

		@Override
		public void handle(Buffer buf) {
			vertx.cancelTimer(timerId);

			event.write(CoreStateListener.current);

			setTimeout();
		}

	}

	@Override
	public void start() {
		NetServer server = vertx.createNetServer();
		server.connectHandler(new Handler<NetSocket>() {
			@Override
			public void handle(NetSocket event) {
				SmtpdSenderRestrictionsBuffer ssrb = new SmtpdSenderRestrictionsBuffer(event);
				ssrb.setTimeout();

				event.handler(RecordParser.newDelimited("\n", ssrb));
			}
		});

		server.listen(25250, "127.0.0.1");
	}

}

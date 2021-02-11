/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.stream;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

public class EmptyStream implements ReadStream<Buffer> {

	@Override
	public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public ReadStream<Buffer> handler(Handler<Buffer> handler) {
		return this;
	}

	@Override
	public ReadStream<Buffer> pause() {
		return this;
	}

	@Override
	public ReadStream<Buffer> resume() {
		return this;
	}

	@Override
	public ReadStream<Buffer> fetch(long amount) {
		return this;
	}

	@Override
	public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
		endHandler.handle(null);
		return this;
	}

}

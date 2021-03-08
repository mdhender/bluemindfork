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
package net.bluemind.imap.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;

public interface IConnectionSupport {

	public interface INetworkCon {

		ReadStream<Buffer> read();

		WriteStream<Buffer> write();

		default void write(String s) {
			write().write(Buffer.buffer(s));
		}

		default void write(Buffer b) {
			write().write(b);
		}

		default void close(Handler<AsyncResult<Void>> h) {
			h.handle(Result.success());
		}

	}

	public void connect(int port, String host, Handler<AsyncResult<INetworkCon>> futureCon);

	Vertx vertx();

}

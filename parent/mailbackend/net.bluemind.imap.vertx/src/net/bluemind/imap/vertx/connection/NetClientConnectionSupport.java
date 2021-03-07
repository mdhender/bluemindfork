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
package net.bluemind.imap.vertx.connection;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.IConnectionSupport;
import net.bluemind.lib.vertx.Result;

public class NetClientConnectionSupport implements IConnectionSupport {

	private final NetClient nc;
	private final Vertx vertx;

	public NetClientConnectionSupport(Vertx vertx, NetClient nc) {
		this.vertx = vertx;
		this.nc = nc;
	}

	@Override
	public Vertx vertx() {
		return vertx;
	}

	@Override
	public void connect(int port, String host, Handler<AsyncResult<INetworkCon>> futureCon) {
		nc.connect(port, host, (AsyncResult<NetSocket> nsRes) -> {
			if (nsRes.failed()) {
				futureCon.handle(Result.fail(nsRes.cause()));
			} else {
				NetSocket ns = nsRes.result();
				INetworkCon netCon = new INetworkCon() {

					@Override
					public WriteStream<Buffer> write() {
						return ns;
					}

					@Override
					public ReadStream<Buffer> read() {
						return ns;
					}

					@Override
					public void close(Handler<AsyncResult<Void>> h) {
						ns.close(h);
					}

				};
				futureCon.handle(Result.success(netCon));
			}
		});
	}

}

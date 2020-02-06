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
package net.bluemind.lmtp.testhelper.common;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

public class WriteSupport {

	private NetSocket sock;

	public WriteSupport(NetSocket sock) {
		this.sock = sock;
	}

	public CompletableFuture<Void> writeWithCRLF(String s) {
		Buffer buf = Buffer.buffer(s).appendString("\r\n");
		return writeRaw(buf);
	}

	public CompletableFuture<Void> writeRaw(Buffer b) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		sock.write(b);
		if (sock.writeQueueFull()) {
			sock.drainHandler(v -> {
				ret.complete(null);
			});
		} else {
			ret.complete(null);
		}
		return ret;
	}

}

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
package net.bluemind.unixsocket.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

import net.bluemind.unixsocket.UnixDomainSocketChannel;

public class UnixChannel implements UnixDomainSocketChannel {

	private int fd;
	private SocketAPI impl;
	private boolean open;

	public UnixChannel(SocketAPI impl, int clientSocket) {
		this.impl = impl;
		this.fd = clientSocket;
		this.open = true;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int max = dst.remaining();

		int v = impl.recv(fd, dst, max, 0);
		dst.position(dst.position() + v);

		return v;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int max = src.remaining();
		int v = impl.send(fd, src, max, 0);
		src.position(src.position() + v);

		return v;
	}

	@Override
	public void close() throws IOException {
		impl.close(fd);
		this.open = false;
	}

	@Override
	public boolean isOpen() {
		return open;
	}
}

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
package net.bluemind.unixsocket;

import java.io.IOException;

import net.bluemind.unixsocket.impl.SocketAPI;

public class UnixClientSocket {

	private int fd;
	private SocketAPI impl;
	private String path;

	public UnixClientSocket(String path) throws IOException {
		this.path = path;
		this.impl = new SocketAPI();
		fd = impl.allocateSocket();
	}

	public UnixDomainSocketChannel connect() throws IOException {
		return impl.connect(fd, path);
	}

	public void close() throws IOException {
		impl.close(fd);
	}

}

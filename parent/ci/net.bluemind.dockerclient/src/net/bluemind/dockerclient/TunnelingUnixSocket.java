/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dockerclient;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

final class TunnelingUnixSocket extends UnixSocket {
	private final File path;
	private InetSocketAddress inetSocketAddress;

	TunnelingUnixSocket(File path, UnixSocketChannel channel) {
		super(channel);
		this.path = path;
	}

	TunnelingUnixSocket(File path, UnixSocketChannel channel, InetSocketAddress address) {
		this(path, channel);
		this.inetSocketAddress = address;
	}

	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		this.inetSocketAddress = (InetSocketAddress) endpoint;
		super.connect(new UnixSocketAddress(path), 0);
	}

	@Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {
		this.inetSocketAddress = (InetSocketAddress) endpoint;
		super.connect(new UnixSocketAddress(path), timeout);
	}

	@Override
	public InetAddress getInetAddress() {
		return inetSocketAddress.getAddress();
	}
}
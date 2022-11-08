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
import java.net.Socket;

import javax.net.SocketFactory;

import jnr.unixsocket.UnixSocketChannel;

public final class UnixDomainSocketFactory extends SocketFactory {
	private final File path;

	public UnixDomainSocketFactory(File path) {
		this.path = path;
	}

	@Override
	public Socket createSocket() throws IOException {
		UnixSocketChannel channel = UnixSocketChannel.open();
		return new TunnelingUnixSocket(path, channel);
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException {
		Socket result = createSocket();
		result.connect(new InetSocketAddress(host, port));
		return result;
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
		return createSocket(host, port);
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException {
		Socket result = createSocket();
		result.connect(new InetSocketAddress(host, port));
		return result;
	}

	@Override
	public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
		return createSocket(host, port);
	}
}

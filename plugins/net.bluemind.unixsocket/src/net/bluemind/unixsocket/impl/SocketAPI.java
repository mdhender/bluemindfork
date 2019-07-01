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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import net.bluemind.unixsocket.UnixDomainSocketChannel;

/**
 * Wraps the low-level C calls in java methods with exception handling.
 * 
 */
public class SocketAPI {

	private static final Logger logger = LoggerFactory.getLogger(SocketAPI.class);

	public SocketAPI() {
	}

	private sockaddr_un createUnixSockAddress(String path) {
		if (path.getBytes().length > 108) {
			throw new RuntimeException("Path length MUST be < 108 (see man unix(7))");
		}
		sockaddr_un ret = new sockaddr_un(DMLibcSocket.AF_UNIX, path.getBytes());
		return ret;
	}

	public int allocateSocket() throws IOException {
		int fd = DMLibcSocket.socket(DMLibcSocket.AF_UNIX, DMLibcSocket.SOCK_STREAM, 0);
		if (fd == -1) {
			perror("socket call failed", Native.getLastError());
			throw new IOException("socket(AF_UNIX, SOCK_STREAM, 0) failed.");
		} else {
			return fd;
		}
	}

	public void bindToPath(int fd, String path) throws IOException {
		File f = new File(path);
		if (f.exists()) {
			if (!f.delete()) {
				throw new IOException(path + " already exists & could not be replaced.");
			}
		}
		sockaddr_un addr = createUnixSockAddress(path);
		// +2 for family + path
		int ret = DMLibcSocket.bind(fd, addr, addr.sun_path.length + 2);
		if (ret == -1) {
			perror("bind failed for path " + path, Native.getLastError());
			throw new IOException("could not bind socket on " + path);
		}
		f.setReadable(true, false);
		f.setWritable(true, false);
	}

	public void close(int fd) throws IOException {
		int ret = DMLibcSocket.close(fd);
		if (ret == -1) {
			perror("cannot close socket " + fd, Native.getLastError());
			throw new IOException("Could not close socket fd " + fd);
		}
	}

	private void perror(String string, int errno) {
		logger.error(string + ": " + DMLibcSocket.strerror(errno));
	}

	public void listen(int fd, int queueLength) throws IOException {
		int ret = DMLibcSocket.listen(fd, queueLength);
		if (ret == -1) {
			perror("Error listening on " + fd, Native.getLastError());
			throw new IOException("Could not listen on socket fd " + fd + " acceptCount: " + queueLength);
		}
	}

	public UnixDomainSocketChannel accept(int fd) throws IOException {
		IntByReference addrLen = new IntByReference();
		sockaddr_un clientAddr = new sockaddr_un(DMLibcSocket.AF_UNIX, new byte[108]);
		int clientSocket = DMLibcSocket.accept(fd, clientAddr, addrLen);
		if (clientSocket == -1) {
			perror("could not accept connection", Native.getLastError());
			throw new IOException("Could not accept connection");
		}
		return new UnixChannel(this, clientSocket);
	}

	public UnixDomainSocketChannel connect(int fd, String path) throws IOException {
		File f = new File(path);
		if (!f.exists()) {
			throw new IOException(path + " does not exists");
		}
		sockaddr_un addr = createUnixSockAddress(path);
		// +2 for family + path
		int ret = DMLibcSocket.connect(fd, addr, addr.sun_path.length + 2);
		if (ret == -1) {
			perror("connect failed for path " + path, Native.getLastError());
			throw new IOException("could not connect to socket at " + path);
		}
		return new UnixChannel(this, fd);
	}

	public int recv(int fd, ByteBuffer dst, int max, int i) throws IOException {
		int ret = DMLibcSocket.recv(fd, dst, max, i);
		if (ret == -1) {
			perror("recv error", Native.getLastError());
			throw new IOException("recv error");
		}
		return ret;
	}

	public int send(int fd, ByteBuffer src, int max, int i) throws IOException {
		int ret = DMLibcSocket.send(fd, src, max, i);
		if (ret == -1) {
			perror("send error", Native.getLastError());
			throw new IOException("send error");
		}
		return ret;
	}
}

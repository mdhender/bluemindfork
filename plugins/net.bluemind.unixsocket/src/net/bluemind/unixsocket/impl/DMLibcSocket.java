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

import java.nio.ByteBuffer;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

/**
 * Low level LibC binding suitable for JNA
 * 
 * 
 */
public class DMLibcSocket {

	// public static final DMLibcSocket INSTANCE = (DMLibcSocket) Native
	// .loadLibrary("c", DMLibcSocket.class);
	static {
		Native.register("c");
	}

	public static final short AF_UNIX = 1;
	public static final short SOCK_STREAM = 1;

	public static native int socket(int domain, int type, int protocol);

	public static native int connect(int s, sockaddr_un name, int namelen);

	public static native int bind(int s, sockaddr_un name, int namelen);

	public static native int listen(int s, int backlog);

	public static native int accept(int s, sockaddr_un addr, IntByReference addrlen);

	public static native int recv(int s, ByteBuffer buf, int len, int flags);

	public static native int send(int s, ByteBuffer msg, int len, int flags);

	public static native int close(int s);

	public static native String strerror(int errno);

}

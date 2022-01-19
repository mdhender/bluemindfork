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
package net.bluemind.imap.sieve;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client API to cyrus sieve server
 * 
 * <code>http://www.ietf.org/proceedings/06mar/slides/ilemonade-1.pdf</code>
 * 
 * 
 */
public class SieveClient implements AutoCloseable {

	private final SieveConnectionData connectionData;
	private final SieveClientSupport cs;
	private static final NioSocketConnector connector;

	static {
		connector = new NioSocketConnector();
		connector.setHandler(new SieveClientHandler());
	}

	private static final Logger logger = LoggerFactory.getLogger(SieveClient.class);

	public SieveClient(SieveConnectionData connectionData) {
		this.connectionData = connectionData;
		cs = new SieveClientSupport(connectionData.login, connectionData.authname, connectionData.password);
	}

	public boolean login() {
		if (logger.isDebugEnabled()) {
			logger.debug("login called");
		}
		SocketAddress sa = new InetSocketAddress(connectionData.host, connectionData.port);
		return cs.login(connector, sa);
	}

	public List<SieveScript> listscripts() {
		return cs.listscripts();
	}

	public boolean putscript(String name, InputStream scriptContent) {
		return cs.putscript(name, scriptContent);
	}

	public void unauthenticate() {
		cs.unauthenticate();
	}

	public void logout() {
		cs.logout();
	}

	public boolean deletescript(String name) {
		return cs.deletescript(name);
	}

	public String getScript(String name) {
		return cs.getScript(name);
	}

	public boolean activate(String newName) {
		return cs.activate(newName);
	}

	public static class SieveConnectionData implements Closeable {
		public final String login;
		public final String authname;
		public final String password;
		public final String host;
		public final int port;
		private final static int DEFAULT_PORT = 2000;

		public SieveConnectionData(String login, String authname, String password, String host, int port) {
			this.login = login;
			this.authname = authname;
			this.password = password;
			this.host = host;
			this.port = port;
		}

		public SieveConnectionData(String login, String password, String host) {
			this(login, login, password, host, DEFAULT_PORT);
		}

		public SieveConnectionData(String username, String authname, String password, String host) {
			this(username, authname, password, host, DEFAULT_PORT);
		}

		@Override
		public void close() throws IOException {
		}

		public String toString() {
			return login + "@" + host + ":" + port;
		}

	}

	@Override
	public void close() {
		logout();
	}

}

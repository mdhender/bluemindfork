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

import java.io.InputStream;
import java.net.SocketAddress;
import java.util.List;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.apache.mina.transport.socket.SocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.sieve.commands.GetScript;
import net.bluemind.imap.sieve.commands.SieveActivate;
import net.bluemind.imap.sieve.commands.SieveAuthenticate;
import net.bluemind.imap.sieve.commands.SieveDeleteScript;
import net.bluemind.imap.sieve.commands.SieveListscripts;
import net.bluemind.imap.sieve.commands.SievePutscript;
import net.bluemind.imap.sieve.commands.SieveUnauthenticate;

public class SieveClientSupport {

	private static final Logger logger = LoggerFactory.getLogger(SieveClientSupport.class);

	private static final long WAIT_SYNC = 2 * 1000; // 2s
	private IoSession session;
	private SieveAuthenticate authenticate;

	public SieveClientSupport(String login, String authname, String password) {
		this.authenticate = new SieveAuthenticate(login, authname, password);
	}

	public boolean login(SocketConnector connector, SocketAddress sa) {
		if (session != null && session.isConnected()) {
			throw new IllegalStateException("Already connected. Disconnect first.");
		}

		try {

			// wait for
			// "IMPLEMENTATION" "Cyrus timsieved v2.2.13-Debian-2.2.13-10"
			// "SASL" "PLAIN"
			// "SIEVE"
			// "fileinto reject envelope vacation imapflags notify subaddress
			// relational comparator-i;ascii-numeric regex"
			// "STARTTLS"
			// OK
			synchronized (this) {
				ConnectFuture cf = connector.connect(sa, new IoSessionInitializer<ConnectFuture>() {
					@Override
					public void initializeSession(IoSession session, ConnectFuture future) {
						logger.debug("init sieve client session");
						session.setAttribute("scs", SieveClientSupport.this);
					}
				});

				this.wait(WAIT_SYNC);

				if (!cf.isConnected()) {
					return false;
				}
				session = cf.getSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Connection established, sending login.");
				}
			}

			return run(authenticate);
		} catch (Exception e) {
			logger.error("login error", e);
			return false;
		}
	}

	public void logout() {
		logger.debug("logout from sieve\n");
		synchronized (this) {
			if (session != null) {
				session.close(true);
				try {
					this.wait(WAIT_SYNC);
				} catch (InterruptedException e) {
				}
			} else {
				logger.warn("logout session was null");
			}
		}

	}

	private <T> T run(SieveCommand<T> cmd) {
		if (logger.isDebugEnabled()) {
			logger.debug("running command " + cmd);
		}
		// grab lock, this one should be ok, except on first call
		// where we might wait for sieve welcome text.
		synchronized (this) {
			cmd.execute(session);
			try {
				this.wait();
				cmd.responseReceived(response());

			} catch (InterruptedException e) {
			}
		}

		return cmd.getReceivedData();
	}

	private SieveResponse response() {
		if (session == null) {
			return null;
		} else {
			return (SieveResponse) session.getAttribute("response");
		}
	}

	public void setResponses(SieveResponse copy) {
		if (logger.isDebugEnabled()) {
			logger.debug("in setResponses on " + Integer.toHexString(hashCode()));
		}
		if (session != null) {
			session.setAttribute("response", copy);
		}

		synchronized (this) {
			this.notifyAll();
		}
	}

	public void sessionClosed() {
		logger.debug("session closed");
		if (session != null) {
			session.setAttribute("response", null);
		}
		session = null;
		synchronized (this) {
			this.notifyAll();
		}
	}

	public List<SieveScript> listscripts() {
		return run(new SieveListscripts());
	}

	public boolean putscript(String name, InputStream scriptContent) {
		return run(new SievePutscript(name, scriptContent));
	}

	public void unauthenticate() {
		run(new SieveUnauthenticate());
	}

	public boolean deletescript(String name) {
		return run(new SieveDeleteScript(name));
	}

	public boolean activate(String newName) {
		return run(new SieveActivate(newName));
	}

	public String getScript(String name) {
		return run(new GetScript(name));
	}

}

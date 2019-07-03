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
package net.bluemind.ysnp.impl;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.unixsocket.UnixDomainSocketChannel;

public class AsyncExecutor extends Thread implements IAuthExecutor {

	private Semaphore lock;
	private boolean stopped;
	private UnixDomainSocketChannel channel;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private ConnectionHandler handler;
	private SaslauthdProtocol dialog;

	public AsyncExecutor(ConnectionHandler handler, SaslauthdProtocol dialog) {
		this.lock = new Semaphore(0);
		this.handler = handler;
		this.dialog = dialog;
	}

	private void lock() {
		try {
			lock.acquire();
		} catch (InterruptedException e) {
		}
	}

	public void run() {
		while (!stopped) {
			lock();
			if (stopped) {
				break;
			}
			doAsyncAuthentificationDialog();
		}
		logger.info("executor stopped.");
	}

	private void doAsyncAuthentificationDialog() {
		if (logger.isDebugEnabled()) {
			logger.debug("doing auth dialog for channel " + channel + "...");
		}

		try {
			dialog.execute(channel);
		} catch (Exception e) {
			logger.error("protocol error during authentification", e);
		} finally {
			try {
				channel.close();
			} catch (IOException e) {
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("doAsyncAuthDialog completed.");
		}
		handler.onExecutionComplete(this);
	}

	@Override
	public void doAuthenticationDialog(UnixDomainSocketChannel channel) {
		this.channel = channel;
		lock.release();
	}

	public void shutdown() {
		stopped = true;
		lock.release();
		try {
			join();
		} catch (InterruptedException e) {
		}
	}

}

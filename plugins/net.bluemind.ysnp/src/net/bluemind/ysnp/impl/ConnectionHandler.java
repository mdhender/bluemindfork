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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.unixsocket.UnixDomainSocketChannel;
import net.bluemind.ysnp.YSNPConfiguration;

/**
 * Maintains a queue of {@link AsyncExecutor} that will run the saslauthd dialog
 * in a separate thread.
 * 
 * 
 */
public class ConnectionHandler implements IConnectionListener {

	private LinkedBlockingQueue<IAuthExecutor> executors;
	private List<IAuthExecutor> inProgress;
	private boolean stopped;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ConnectionHandler(ValidationPolicy vp, YSNPConfiguration configuration) {
		int cnt = configuration.getExecutorsCount();
		ArrayList<IAuthExecutor> le = new ArrayList<IAuthExecutor>(cnt);
		AuthExecutorFactory aef = new AuthExecutorFactory(vp);
		for (int i = 0; i < cnt; i++) {
			le.add(aef.createAsyncExecutor(this));
		}
		executors = new LinkedBlockingQueue<IAuthExecutor>(le);
		inProgress = Collections.synchronizedList(new LinkedList<IAuthExecutor>());
	}

	@Override
	public void connectionAccepted(UnixDomainSocketChannel channel) {
		// find an available executor
		IAuthExecutor executor = null;
		while (!stopped && executor == null) {

			try {
				executor = executors.poll(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.error("interrupted while fetching an executor", e);
			}

			if (executor != null) {
				inProgress.add(executor);
				executor.doAuthenticationDialog(channel);
			} else {
				logger.warn("Takes more than 1sec to find an executor.");
			}
		}
	}

	public void onExecutionComplete(IAuthExecutor done) {
		boolean sucess = executors.add(done);
		if (logger.isDebugEnabled()) {
			logger.debug("adding executor back to queue (" + sucess + ")");
		}
		inProgress.remove(done);
	}

	public void shutdown() {
		stopped = true;
		while (!executors.isEmpty()) {
			IAuthExecutor e = executors.remove();
			e.shutdown();
		}
		for (IAuthExecutor e : inProgress) {
			e.shutdown();
		}
	}

}

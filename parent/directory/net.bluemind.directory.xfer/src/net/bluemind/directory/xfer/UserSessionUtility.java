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
package net.bluemind.directory.xfer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class UserSessionUtility implements ISessionUtility, AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(UserSessionUtility.class);

	private final String dataLocation;
	private final String userLatd;

	private final BmContext context;
	private boolean isLockedOut;

	public UserSessionUtility(BmContext context, String userLatd, String dataLocation) {
		this.userLatd = userLatd;
		this.dataLocation = dataLocation;
		this.context = context;

		this.isLockedOut = false;
	}

	private void releaseUser() {
		logger.info("Release user {}", userLatd);
		ItemValue<Server> server = Topology.get().datalocation(dataLocation);
		INodeClient nc = NodeActivator.get(server.value.address());
		logger.info("Allowing user {} again on server {}", userLatd, server.displayName);
		NCUtils.exec(nc, "cyr_deny -a " + userLatd, 10, TimeUnit.SECONDS);
	}

	public void logoutUser(IServerTaskMonitor monitor) {
		logger.info("Logging out user {}", userLatd);
		monitor.log("Logging out user " + userLatd);
		ISessionsMgmt sessionApi = ServerSideServiceProvider.getProvider(context).instance(ISessionsMgmt.class);
		sessionApi.logoutUser(userLatd);
	}

	public void lockoutUser(IServerTaskMonitor monitor) {
		logger.info("Kill imap sessions of {}", userLatd);

		ItemValue<Server> server = Topology.get().datalocation(dataLocation);
		INodeClient nc = NodeActivator.get(server.value.address());

		// Don't allow new connections
		isLockedOut = true;
		NCUtils.exec(nc, "cyr_deny -m xfer-in-progress " + userLatd, 1, TimeUnit.MINUTES);

		// Retrieve existing imap sessions
		ExitList exit = NCUtils.exec(nc, "cyr_info proc", 1, TimeUnit.MINUTES);
		List<String> killPids = new ArrayList<>();
		for (String line : exit) {
			// pid service servername [ip] latd@domainUid blue-mind.net!user.thomas^fricker
			List<String> lineSplitted = Splitter.on(" ").trimResults().splitToList(line);
			String pid = lineSplitted.get(0);
			String latd = lineSplitted.get(4);
			if (userLatd.equals(latd)) {
				killPids.add(pid);
			}
		}

		// Kill sessions if any
		if (!killPids.isEmpty()) {
			logger.info("Sending SIGTERM signal to {} (kill sessions of {})", killPids, userLatd);
			NCUtils.exec(nc, "kill -SIGTERM " + killPids.stream().collect(Collectors.joining(" ")), 30,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void close() throws Exception {
		if (isLockedOut) {
			this.releaseUser();
		}
	}

}

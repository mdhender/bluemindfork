/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.core.backup.continuous.IRecordStarvationStrategy;
import net.bluemind.core.backup.continuous.restore.InstallFromBackupTask.ClonedOrphans;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CloneConfiguration;
import net.bluemind.system.api.IInstallation;

public class RecordStarvationHandler implements IRecordStarvationStrategy {

	private boolean demoteAsked = false;
	private final Set<String> starvedTopics;
	private final IServerTaskMonitor monitor;
	private int goal;
	private CloneConfiguration cloneConf;
	private ClonedOrphans orphans;

	public RecordStarvationHandler(IServerTaskMonitor forLogs, CloneConfiguration cloneConf, ClonedOrphans orphans) {
		this.monitor = forLogs;
		starvedTopics = ConcurrentHashMap.newKeySet();
		this.cloneConf = cloneConf;
		this.orphans = orphans;
		this.goal = orphans.domains.size();
	}

	@Override
	public synchronized ExpectedBehaviour onStarvation(JsonObject json) {
		monitor.log(json.encode() + " RECORD STARVATION");
		starvedTopics.add(json.getString("topic"));
		System.err.println(json.encode() + " has completed, starved: " + starvedTopics.size() + " out of " + goal);
		if (starvedTopics.size() < goal) {
			System.err.println("We have " + starvedTopics.size() + " starved topics, we expect " + goal);
			return ExpectedBehaviour.RETRY;
		}

		switch (cloneConf.mode) {
		case PROMOTE:
			if (!demoteAsked) {
				// connect to upstream
				ItemValue<Server> leaderCore = orphans.topology.values().stream().map(ps -> ps.leader)
						.filter(iv -> iv.value.tags.contains("bm/core")).findFirst().orElse(null);
				monitor.log("Ask active leader " + leaderCore + " to relinquish control....");
				if (leaderCore != null) {
					String url = "http://" + leaderCore.value.address() + ":8090";
					ClientSideServiceProvider prov = ClientSideServiceProvider.getProvider(url, null);
					IAuthentication authApi = prov.instance(IAuthentication.class);
					LoginResponse auth = authApi.login("admin0@global.virt", "admin", "clone-demote");
					if (auth.status != Status.Ok) {
						System.err.println("Failed auth on " + url + " -> " + auth);
						System.exit(1);
					}
					prov = ClientSideServiceProvider.getProvider(url, auth.authKey);
					IInstallation masterInstApi = prov.instance(IInstallation.class, cloneConf.sourceInstallationId);
					monitor.log("Calling demote....");
					masterInstApi.demoteLeader();
				}
				demoteAsked = true;
				// run another kafka readloop & expect a BYE in topic
				return ExpectedBehaviour.RETRY;
			} else {
				monitor.log("New starvation after demote was called !!!");
				// assume we're good
				return ExpectedBehaviour.ABORT;
			}
		case FORK:
			return ExpectedBehaviour.ABORT;
		case TAIL:
		default:
			System.err.println("loop for mode " + cloneConf.mode);
			return ExpectedBehaviour.RETRY;
		}

	}

}

/*BEGIN LICENSE
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
package net.bluemind.system.iptables.tools;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.iptables.cf.BmIptablesRules;

public class RulesUpdater {
	private static final Logger logger = LoggerFactory.getLogger(RulesUpdater.class);
	private static final IServerTaskMonitor FAKE_MONITOR = new IServerTaskMonitor() {
		@Override
		public IServerTaskMonitor subWork(String logPrefix, double work) {
			return this;
		}

		@Override
		public IServerTaskMonitor subWork(double work) {
			return this;
		}

		@Override
		public void progress(double doneWork, String log) {
		}

		@Override
		public void log(String log) {
		}

		@Override
		public void end(boolean success, String log, String result) {
		}

		@Override
		public void begin(double totalWork, String log) {
		}
	};

	public static void updateIptablesScript(BmContext bc, Server removedHost, Server newHost) throws ServerFault {
		updateIptablesScript(bc.su().provider(), FAKE_MONITOR, removedHost, newHost);
	}

	public static void updateIptablesScript() throws ServerFault {
		updateIptablesScript(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), FAKE_MONITOR, null, null);
	}

	public static void updateIptablesScript(IServerTaskMonitor monitor) throws ServerFault {
		updateIptablesScript(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), monitor, null, null);
	}

	private static void updateIptablesScript(IServiceProvider sp, IServerTaskMonitor monitor, Server removedHost,
			Server newHost) throws ServerFault {
		IServer srvApi = sp.instance(IServer.class, InstallationId.getIdentifier());
		monitor.begin(10, "");

		List<ItemValue<Server>> hosts = srvApi.allComplete();
		HashSet<String> hostsAddresses = new HashSet<String>(hosts.size() + 1);
		for (ItemValue<Server> host : hosts) {
			hostsAddresses.add(host.value.address());
		}

		if (removedHost != null) {
			// Ensure old host is not in list
			hostsAddresses.remove(removedHost.address());
		}

		if (newHost != null) {
			// Ensure new host address is in list
			hostsAddresses.add(newHost.address());
		}

		addAdditionalAddresses(sp, hostsAddresses);

		monitor.progress(1, "Server list retrieved");

		IServerTaskMonitor progress = monitor.subWork(9);
		progress.begin(hosts.size(), "Updating servers");

		BmIptablesRules bmIptablesRules = new BmIptablesRules(hostsAddresses);
		for (ItemValue<Server> host : hosts) {
			logger.info("Updating iptables script on node: " + host.value.address());
			try {
				bmIptablesRules.write(NodeActivator.get(host.value.address()));
				progress.progress(1, "Host " + host.value.address() + " updated");
			} catch (ServerFault e) {
				logger.warn("error during iptables write", e);
				progress.progress(1, "Host " + host.value.address() + " failed updated");
			}

		}

	}

	private static void addAdditionalAddresses(IServiceProvider ssp, Set<String> hostsAddresses) throws ServerFault {
		String fwAdditionalIPs = ssp.instance(ISystemConfiguration.class, InstallationId.getIdentifier()).getValues()
				.stringValue(SysConfKeys.fwAdditionalIPs.name());

		if (fwAdditionalIPs != null && !fwAdditionalIPs.trim().isEmpty()) {
			String[] ips = fwAdditionalIPs.split(" ");
			hostsAddresses.addAll(Arrays.asList(ips));
		}
	}
}

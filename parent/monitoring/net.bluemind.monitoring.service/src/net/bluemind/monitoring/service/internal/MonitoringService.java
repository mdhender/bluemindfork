/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2014
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.service.internal;

import net.bluemind.core.rest.BmContext;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.Config;
import net.bluemind.monitoring.api.IMonitoring;
import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.PluginsList;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.server.api.Server;

public class MonitoringService implements IMonitoring {

	private BmContext context;

	public MonitoringService(BmContext context) {
		this.context = context;
	}

	@Override
	public PluginsList getPluginsInfo() throws Exception {
		PluginsList plugins = new PluginsList();

		for (ServiceInfoProvider sip : MonitoringServiceActivator.getProviders().values()) {
			plugins.add(sip.impl.getPluginInfo());
		}

		plugins.postProcess();

		return plugins;
	}

	@Override
	public PluginInformation getPluginInfo(String plugin) throws Exception {
		return MonitoringServiceActivator.getProvider(plugin).get().impl.getPluginInfo();
	}

	@Override
	public ServiceInformation getServiceInfo(String plugin, String service) throws Exception {
		return MonitoringServiceActivator.getProvider(plugin).get().impl.getServiceInfo(service);
	}

	@Override
	public MethodInformation getMethodInfo(String plugin, String service, String method) throws Exception {
		return MonitoringServiceActivator.getProvider(plugin).get().impl.getMethodInfo(service, method);
	}

	@Override
	public ServerInformation getServerInfo(String plugin, String service, String method, String server)
			throws Exception {

		Server s = CommandExecutor.getServerByIp(server);

		return MonitoringServiceActivator.getProvider(plugin).get().impl.getServerInfo(service, method, s);
	}

	@Override
	public Config getConfig() throws Exception {
		// execute only on 'main' server
		Server server = CommandExecutor.getAllServers().stream().filter(s -> s.tags.contains("bm/core")).findFirst().get();
		
		Command cpu = new Command("/usr/share/bm-node/monitoring/cpuinfo.sh");
		Command mem = new Command("/usr/share/bm-node/monitoring/meminfo.sh");
		Config conf = new Config();

		CommandExecutor.execCmdOnServer(server, cpu);
		CommandExecutor.execCmdOnServer(server, mem);

		conf.addPart(cpu.rawData);
		conf.addPart(mem.rawData);

		return conf;
	}

}

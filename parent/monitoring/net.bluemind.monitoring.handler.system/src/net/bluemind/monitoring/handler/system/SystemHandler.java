/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.monitoring.handler.system;

import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.monitoring.service.IServiceInfoProvider;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public class SystemHandler implements IServiceInfoProvider {

	public static String SCRIPTS_FOLDER = "/usr/share/bm-node/monitoring/system/";
	public static String BASE = "system";

	@Override
	public PluginInformation getPluginInfo() throws Exception {
		PluginInformation infoList = new PluginInformation("system");

		infoList.addInformation(getServiceInfo("cpu"));
		infoList.addInformation(getServiceInfo("disks"));
		infoList.addInformation(getServiceInfo("dns"));
		infoList.addInformation(getServiceInfo("leaks"));
		infoList.addInformation(getServiceInfo("memory"));
		infoList.addInformation(getServiceInfo("mailindex"));

		infoList.postProcess();

		return infoList;
	}

	@Override
	public ServiceInformation getServiceInfo(String service) throws Exception {

		ServiceInformation info = null;

		info = this.getServiceInstance(service).getServiceInformation();

		return info;
	}

	@Override
	public MethodInformation getMethodInfo(String service, String method) throws Exception {

		MethodInformation info = null;

		info = this.getServiceInstance(service).getMethodInformation(method);

		return info;

	}

	@Override
	public ServerInformation getServerInfo(String service, String method, Server server) throws Exception {
		return this.getServiceInstance(service).getServerInfo(server, method);
	}

	@Override
	public Service getServiceInstance(String service) {
		Service s = null;
		switch (service) {
		case "disks":
			s = Disks.getInstance();
			break;
		case "leaks":
			s = Leaks.getInstance();
			break;
		case "dns":
			s = Dns.getInstance();
			break;
		case "memory":
			s = Memory.getInstance();
			break;
		case "cpu":
			s = Cpu.getInstance();
			break;
		case "mailindex":
			s = MailIndex.getInstance();
			break;
		}
		return s;
	}

}

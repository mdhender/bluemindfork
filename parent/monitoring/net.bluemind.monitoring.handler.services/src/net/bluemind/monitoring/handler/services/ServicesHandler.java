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
package net.bluemind.monitoring.handler.services;

import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.monitoring.service.IServiceInfoProvider;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public class ServicesHandler implements IServiceInfoProvider {

	public static String SCRIPTS_FOLDER = "/usr/share/bm-node/monitoring/services/";
	public static String BASE = "services";

	@Override
	public PluginInformation getPluginInfo() throws Exception {
		PluginInformation list = new PluginInformation("services");

		for (BmService service : BmService.values()) {
			list.addInformation(getServiceInfo(service.getServiceName()));
		}

		list.postProcess();

		return list;
	}

	@Override
	public ServiceInformation getServiceInfo(String service) throws Exception {
		return this.getServiceInstance(service).getServiceInformation();
	}

	@Override
	public MethodInformation getMethodInfo(String service, String method) throws Exception {
		return this.getServiceInstance(service).getMethodInformation(method);
	}

	@Override
	public ServerInformation getServerInfo(String service, String method, Server server) throws Exception {
		return this.getServiceInstance(service).getServerInfo(server, method);
	}

	@Override
	public Service getServiceInstance(String service) {

		Service s = null;

		try {
			s = ServicesHandler.generateService(service);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return s;
	}

	public static AbstractService generateService(String service)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		String className = "net.bluemind.monitoring.handler.services." + service.substring(0, 1).toUpperCase()
				+ service.substring(1);

		AbstractService s = (AbstractService) Class.forName(className).newInstance();

		return s;
	}
}

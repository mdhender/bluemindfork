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
package net.bluemind.monitoring.service;

import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

/**
 * Interface that every information provider must implement in order to fetch
 * information
 * 
 * @author vincent
 *
 */
public interface IServiceInfoProvider {

	/**
	 * 
	 * @return a plugin information containing every service information
	 * @throws Exception
	 */
	public PluginInformation getPluginInfo() throws Exception;

	/**
	 * 
	 * @param service
	 *            the service to be monitored
	 * @return a service information of the specified service containing every
	 *         method information
	 * @throws Exception
	 */
	public ServiceInformation getServiceInfo(String service) throws Exception;

	/**
	 * 
	 * @param service
	 *            the service to be monitored
	 * @param method
	 *            the method to be executed
	 * @return a method information containing every server-specific information
	 * @throws Exception
	 */
	public MethodInformation getMethodInfo(String service, String method) throws Exception;

	/**
	 * 
	 * @param service
	 *            the service to be monitored
	 * @param method
	 *            the method to be executed
	 * @param server
	 *            the server on which the method must be executed
	 * @return a server information with messages and fetched data
	 * @throws Exception
	 */
	public ServerInformation getServerInfo(String service, String method, Server server) throws Exception;

	/**
	 * 
	 * @param service
	 *            the service to be returned
	 * @return the service
	 */
	public Service getServiceInstance(String service);

}

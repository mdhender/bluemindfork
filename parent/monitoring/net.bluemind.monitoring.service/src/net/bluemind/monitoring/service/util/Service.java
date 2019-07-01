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
package net.bluemind.monitoring.service.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.server.api.Server;

/**
 * 
 * @author vincent
 *
 */
public abstract class Service {

	public static final Logger logger = LoggerFactory.getLogger(Service.class);
	public String plugin;
	public String service;
	public List<String> tags;
	public List<String> endpoints;

	/**
	 * Creates a new service
	 * 
	 * @param plugin
	 *            the plugin to which the service belongs
	 * @param service
	 *            the name of the service
	 * @param tags
	 *            the tag identifying the servers on which the service can be
	 *            checked
	 */
	public Service(String plugin, String service, List<String> tags) {
		this.plugin = plugin;
		this.service = service;
		this.tags = tags;
		this.endpoints = new ArrayList<String>();
	}
	
	/**
	 * Creates a new service
	 * 
	 * @param plugin
	 *            the plugin to which the service belongs
	 * @param service
	 *            the name of the service
	 * @param tag
	 *            the tag identifying the servers on which the service can be
	 *            checked
	 */
	public Service(String plugin, String service, String tag) {
		this(plugin, service, ImmutableList.of(tag));
	}
	
	/**
	 * Creates a new service
	 * 
	 * @param plugin
	 *            the plugin to which the service belongs
	 * @param service
	 *            the name of the service
	 */
	public Service(String plugin, String service) {
		this(plugin, service, (List<String>)null);
	}

	/**
	 * 
	 * @return a service information with every method information and server
	 *         information for the current service
	 * @throws ServerFault
	 */
	public ServiceInformation getServiceInformation() throws ServerFault {
		ServiceInformation serviceInfo = new ServiceInformation("plugin", service);

		for (String endpoint : this.endpoints) {
			serviceInfo.addInformation(this.getMethodInformation(endpoint));
		}

		serviceInfo.postProcess();

		return serviceInfo;

	}

	/**
	 * 
	 * @param endpoint
	 * @return a method information for the specified endpoint
	 * @throws ServerFault
	 */
	public MethodInformation getMethodInformation(String endpoint) throws ServerFault {
		MethodInformation methodInfo = new MethodInformation(this.plugin, this.service, endpoint);
		List<Server> servers = CommandExecutor.getAllServers();

		for (Server server : servers) {
			if (tags == null || !Collections.disjoint(server.tags, this.tags)) {
				methodInfo.addInformation(this.getServerInfo(server, endpoint));
			}
		}

		methodInfo.postProcess();

		return methodInfo;

	}

	/**
	 * Each service has its own methods and server infos; the lowest level of
	 * information being the server information, the implementation of this
	 * method obviously differs for every service.
	 * 
	 * @param server
	 *            the server on which the method must be executed
	 * @param endpoint
	 *            the method to be executed
	 * @return the server information generated
	 */
	public abstract ServerInformation getServerInfo(Server server, String endpoint);

}

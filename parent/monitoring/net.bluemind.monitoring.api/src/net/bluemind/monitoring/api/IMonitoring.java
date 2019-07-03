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
package net.bluemind.monitoring.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * The Monitoring API entry point.
 */
@BMApi(version = "3")
@Path("/monitoring")
public interface IMonitoring {

	/**
	 * Fetches a list of all information (plugin information, service information,
	 * method information, server information) from every monitoring plugin.
	 * 
	 * @return a plugins list with the complete list of information
	 */
	@GET
	@Path("_all")
	public PluginsList getPluginsInfo() throws Exception;

	/**
	 * TMP: fetches CPU and RAM information.
	 */
	@GET
	@Path("_config")
	public Config getConfig() throws Exception;

	/**
	 * Fetches all information from a single given plugin.
	 * 
	 * @param plugin the plugin from which the information must be fetched
	 * @return all information from the given plugin
	 */
	@GET
	@Path("{plugin}")
	public PluginInformation getPluginInfo(@PathParam("plugin") String plugin) throws Exception;

	/**
	 * Fetches all information from a service belonging to a given plugin.
	 * 
	 * @param plugin  the plugin containing the service
	 * @param service the service from which the information must be fetched
	 * @return all the information from the given service
	 */
	@GET
	@Path("{plugin}/{service}")
	public ServiceInformation getServiceInfo(@PathParam("plugin") String plugin, @PathParam("service") String service)
			throws Exception;

	/**
	 * Fetches all information from a method belonging to a given service.
	 * 
	 * @param plugin  the plugin containing the service
	 * @param service the service containing the method
	 * @param method  the method from which the information must be fetched
	 * @return all the information from the given method
	 */
	@GET
	@Path("{plugin}/{service}/{method}")
	public MethodInformation getMethodInfo(@PathParam("plugin") String plugin, @PathParam("service") String service,
			@PathParam("method") String method) throws Exception;

	/**
	 * Fetches information from a given method belonging to a specific server.
	 * 
	 * @param plugin  the plugin containing the service
	 * @param service the service containing the method
	 * @param method  the method to be used
	 * @param server  the server from which the information must be fetched
	 * @return information from a given method belonging to a specific server
	 */
	@GET
	@Path("{plugin}/{service}/{method}/{server}")
	public ServerInformation getServerInfo(@PathParam("plugin") String plugin, @PathParam("service") String service,
			@PathParam("method") String method, @PathParam("server") String server) throws Exception;

}
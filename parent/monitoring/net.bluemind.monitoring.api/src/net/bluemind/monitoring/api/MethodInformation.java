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

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * A method information contains a list of server information and belongs to a
 * service information.
 */
@BMApi(version = "3")
public class MethodInformation {

	public String plugin;

	public String service;

	public String endpoint;

	public Status status;

	public List<ServerInformation> serverInfoList;

	public MethodInformation() {

	}

	public MethodInformation(String plugin, String service, String endpoint) {
		this.plugin = plugin;
		this.service = service;
		this.endpoint = endpoint;
		this.status = Status.UNKNOWN;
	}

	/**
	 * Updates the status of the method information and removes all the server info
	 * commands. Call this method before returning the MethodInformation.
	 */
	public void postProcess() {

		if (serverInfoList != null) {
			for (ServerInformation srvInfo : this.serverInfoList) {
				if (this.status.getValue() < srvInfo.status.getValue()) {
					this.status = srvInfo.status;
				}
				srvInfo.commands = null;
			}

		}

	}

	/**
	 * Adds a new server information. Prefer this method over
	 * .serverInfoList.add(...).
	 * 
	 * @param info the info to be added
	 */
	public void addInformation(ServerInformation info) {
		if (this.serverInfoList == null) {
			this.serverInfoList = new ArrayList<ServerInformation>();
		}

		if (info != null) {
			this.serverInfoList.add(info);
		}
	}

}

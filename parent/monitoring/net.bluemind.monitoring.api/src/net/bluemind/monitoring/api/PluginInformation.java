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
 * A plugin information is the topmost level of information and contains a list
 * of service information.
 */
@BMApi(version = "3")
public class PluginInformation {

	public String plugin;
	public Status status;
	public List<ServiceInformation> serviceInfoList;

	public PluginInformation() {

	}

	public PluginInformation(String plugin) {
		this.plugin = plugin;
		this.status = Status.UNKNOWN;
	}

	/**
	 * Adds a new ServiceInformation to the PluginInformation.
	 * 
	 * @param info the info to be added
	 */
	public void addInformation(ServiceInformation info) {
		if (this.serviceInfoList == null) {
			this.serviceInfoList = new ArrayList<ServiceInformation>();
		}

		this.serviceInfoList.add(info);
	}

	/**
	 * Updates the status of the plugin information. Call this method before
	 * returning the PluginInformation.
	 */
	public void postProcess() {
		if (this.serviceInfoList != null) {
			for (ServiceInformation serviceInfo : this.serviceInfoList) {
				this.status = (serviceInfo.status.getValue() > this.status.getValue() ? serviceInfo.status
						: this.status);
			}
		}
	}
}

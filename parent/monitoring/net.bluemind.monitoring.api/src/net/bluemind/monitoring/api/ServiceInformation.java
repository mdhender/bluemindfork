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
 * A service information belongs to a plugin information and holds a list of
 * method information.
 */
@BMApi(version = "3")
public class ServiceInformation {

	public String plugin;
	public String service;

	public Status status;
	public List<MethodInformation> methodInfoList;

	public ServiceInformation() {
	}

	public ServiceInformation(String plugin, String service) {
		this.plugin = plugin;
		this.service = service;
		this.status = Status.UNKNOWN;
	}

	/**
	 * Updates the status of the information.
	 */
	public void postProcess() {
		for (MethodInformation info : this.methodInfoList) {
			if (this.status.getValue() < info.status.getValue()) {
				this.status = info.status;
			}
		}
	}

	/**
	 * Adds a new Information to the list. Used this method over
	 * .methodInfoList.add(...).
	 */
	public void addInformation(MethodInformation infoList) {
		if (this.methodInfoList == null) {
			this.methodInfoList = new ArrayList<MethodInformation>();
		}

		if (infoList != null) {
			this.methodInfoList.add(infoList);
		}
	}
}

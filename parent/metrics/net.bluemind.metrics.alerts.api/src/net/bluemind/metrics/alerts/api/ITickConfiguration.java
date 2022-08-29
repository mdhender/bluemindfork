/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.metrics.alerts.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.task.api.TaskRef;

/**
 * TICK stack configuration entry point. <br>
 * <i>Note: TICK stands for Telegraf Influxdb Chronograf Kapacitor.</i>
 * 
 * @see https://www.influxdata.com/time-series-platform/
 */
@BMApi(version = "3", internal = true)
@Path("/tick/mgmt")
public interface ITickConfiguration {

	/**
	 * Updates all TICK component configuration (configures Telegraf polling on all
	 * nodes, publishes metrics dashboard to Chronograf, re-creates every Kapacitor
	 * scripts).
	 */
	@POST
	@Path("_reconfigure")
	TaskRef reconfigure();

}

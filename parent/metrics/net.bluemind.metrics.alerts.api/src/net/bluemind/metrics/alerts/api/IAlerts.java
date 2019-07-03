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

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;

/**
 * Bluemind internal API for the communication between Kapacitor and bm-core.
 * 
 * @see https://www.influxdata.com/time-series-platform/
 */
@BMApi(version = "3", internal = true)
@Path("/alerts")
public interface IAlerts {

	/**
	 * Send an alert. Used by Kapacitor to push an alert to bm-core.
	 * 
	 * @param alertPayload the alert content as a {@link Stream}
	 */
	@POST
	void receive(Stream alertPayload);

}

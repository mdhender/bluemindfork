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
package net.bluemind.metrics.core.tick.dashboards;

import java.io.InputStream;

import net.bluemind.metrics.core.tick.ITickDashboardProvider;

public class MAPIDashboard implements ITickDashboardProvider {

	@Override
	public String name() {
		return "MAPI Insights";
	}

	@Override
	public InputStream content() {
		return MAPIDashboard.class.getClassLoader().getResourceAsStream("dashboards/mapi_insights.json");
	}

}

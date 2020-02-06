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
package net.bluemind.metrics.core.tick;

import java.io.InputStream;
import java.util.List;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Server;

/**
 * Tick template interface, implement it to provide new alerts. It's highly
 * recommended to use existing tickscripts and implementations to create a new
 * one.
 * 
 * @author qvigand
 *
 */
public interface ITickTemplateProvider {

	String templateId();

	InputStream content();

	/**
	 * One TemplateDefinition correspond to one alert. One Template may provide
	 * several definitions.
	 * 
	 * @author qvigand
	 *
	 */
	public static class TemplateDefinition {

		public TemplateDefinition(String string) {
			this.name = string;
		}

		public JsonObject variables = new JsonObject();
		public final String name;
	}

	/**
	 * 
	 * @param ctx
	 * @param endPointUrl Used to send alerts back to Bluemind product for
	 *                    self-healing.
	 * @param server      Server where the alerts are created. AlertsVerticle
	 *                    iterates over all servers, you can use the enum Product to
	 *                    filter the servers where your alert is created.
	 * @return All alerts created for the server.
	 */
	List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server);
}

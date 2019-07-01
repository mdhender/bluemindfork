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
package net.bluemind.metrics.core.tick.scripts;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.BasicTickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.Server;

public class StateHeartbeats extends BasicTickTemplateProvider {

	private static final Logger logger = LoggerFactory.getLogger(StateHeartbeats.class);

	@Override
	public String templateId() {
		return "state-heartbeat-age";
	}

	@Override
	public InputStream content() {
		return StateHeartbeats.class.getClassLoader().getResourceAsStream("tickconfig/state-heartbeat-age.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		return server.value.tags.stream()//
				.flatMap(tag -> Product.byTag(tag).stream())//
				.filter(p -> p.useHearbeats)//
				.map(product -> {
					String alertId = TickTemplateHelper.newId(product, "heartbeat", server);
					TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
							.withEndPoint(endPointUrl).withProduct(product).build();
					logger.info("Definition is {}", def.variables.encodePrettily());
					return def;
				}).collect(Collectors.toList());
	}

}

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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.ITickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class DbConnections implements ITickTemplateProvider {
	private static final Logger logger = LoggerFactory.getLogger(ITickTemplateProvider.class);

	@Override
	public String templateId() {
		return "db-connections";
	}

	@Override
	public InputStream content() {
		return DbConnections.class.getClassLoader().getResourceAsStream("tickconfig/db-connections.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		List<TemplateDefinition> defs = new ArrayList<>();

		if (srvProducts.contains(Product.POSTGRESQL)) {
			if (Topology.get().core().uid.equals(server.uid)) {
				defs.add(addDefinition(endPointUrl, server,
						TickTemplateHelper.newId(Product.POSTGRESQL, templateId() + ".bj", server), "directory"));
			}
			defs.add(addDefinition(endPointUrl, server,
					TickTemplateHelper.newId(Product.POSTGRESQL, templateId() + ".bj-data", server), "mailbox"));
		}
		return defs;
	}

	private TemplateDefinition addDefinition(String endPointUrl, ItemValue<Server> server, String alertId,
			String kind) {
		TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
				.withEndPoint(endPointUrl).withProduct(Product.POSTGRESQL).build();

		def.variables.putObject("targetdatalocation",
				new JsonObject().putString("type", "string").putString("value", server.uid));
		def.variables.putObject("dbkind", new JsonObject().putString("type", "string").putString("value", kind));

		return def;
	}

}

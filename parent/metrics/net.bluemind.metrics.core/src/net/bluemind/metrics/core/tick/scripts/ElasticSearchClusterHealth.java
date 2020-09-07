/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.tick.ITickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.Server;

public class ElasticSearchClusterHealth implements ITickTemplateProvider {
	private static final String NAME = "elasticsearch-cluster-health";

	@Override
	public String templateId() {
		return NAME;
	}

	@Override
	public InputStream content() {
		return ElasticSearchClusterHealth.class.getClassLoader().getResourceAsStream("tickconfig/" + NAME + ".tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		List<TemplateDefinition> defs = new ArrayList<>();
		if (srvProducts.contains(Product.ES)) {
			String alertId = TickTemplateHelper.newId(Product.ES, NAME, server);
			TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
					.withEndPoint(endPointUrl).withProduct(Product.ES).build();
			defs.add(def);
		}
		return defs;
	}
}

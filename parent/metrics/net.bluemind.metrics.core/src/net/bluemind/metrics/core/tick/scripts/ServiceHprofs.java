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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.Product;
import net.bluemind.metrics.core.Product.Family;
import net.bluemind.metrics.core.tick.BasicTickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.Server;

public class ServiceHprofs extends BasicTickTemplateProvider {

	private static final Logger logger = LoggerFactory.getLogger(ServiceHprofs.class);

	@Override
	public String templateId() {
		return "bm-product-hprof";
	}

	@Override
	public InputStream content() {
		return ServiceHprofs.class.getClassLoader().getResourceAsStream("tickconfig/hprof.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));
		srvProducts.add(Product.NODE);

		List<TemplateDefinition> defs = new ArrayList<>(srvProducts.size());

		srvProducts.forEach(product -> {
			if (product.family == Family.JVM) {
				String alertId = TickTemplateHelper.newId(product, "hprof", server);
				TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
						.withEndPoint(endPointUrl).withProduct(product).build();
				logger.info("Definition is {}", def.variables.encodePrettily());
				defs.add(def);
			}
		});
		return defs;
	}

}

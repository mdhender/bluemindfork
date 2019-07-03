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
import net.bluemind.metrics.core.tick.BasicTickTemplateProvider;
import net.bluemind.metrics.core.tick.TickTemplateDefBuilder;
import net.bluemind.metrics.core.tick.TickTemplateHelper;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;

public class ImapConnectionsLimit extends BasicTickTemplateProvider {
	private static final Logger logger = LoggerFactory.getLogger(ImapConnectionsLimit.class);

	@Override
	public String templateId() {
		return "imap-connections";
	}

	@Override
	public InputStream content() {
		return ImapConnectionsLimit.class.getClassLoader()
				.getResourceAsStream("tickconfig/imap-connections-limit.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		List<TemplateDefinition> defs = new ArrayList<TemplateDefinition>();

		ISystemConfiguration sysConfApi = ctx.provider().instance(ISystemConfiguration.class);
		Integer maxChild = sysConfApi.getValues().integerValue("imap_max_child");
		maxChild = maxChild == null ? 200 : maxChild;
		if (srvProducts.contains(Product.CYRUS)) {
			String alertId = TickTemplateHelper.newId(Product.CYRUS, "imap-connections", server);
			TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
					.withEndPoint(endPointUrl).withProduct(Product.CYRUS).withVariable("maxVar", maxChild * 90 / 100)
					.build();
			defs.add(def);
			logger.info("Alerting when maxChild > {}", maxChild * 90 / 100);
		}
		return defs;
	}

}

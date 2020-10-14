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

public class PostfixQueueSize extends BasicTickTemplateProvider {

	private static final Logger logger = LoggerFactory.getLogger(PostfixQueueSize.class);
	private static final String[] queueTypes = { "active", "deferred", "hold", "incoming", "maildrop" };

	@Override
	public String templateId() {
		return "postfix-queue-size";
	}

	@Override
	public InputStream content() {
		return PostfixQueueSize.class.getClassLoader().getResourceAsStream("tickconfig/postfix-queue-size.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		List<TemplateDefinition> defs = new ArrayList<TemplateDefinition>();

		ISystemConfiguration sysConfApi = ctx.provider().instance(ISystemConfiguration.class);
		if (srvProducts.contains(Product.POSTFIX)) {
			Integer maxSize = sysConfApi.getValues().integerValue("message_size_limit");
			maxSize = maxSize == null ? 100000000 : maxSize * 10;
			logger.info("Alerting when emails Size > {}", maxSize);
			for (String queueType : queueTypes) {
				String alertId = TickTemplateHelper.newId(Product.POSTFIX, "postfix-queue-size-" + queueType, server);
				TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
						.withEndPoint(endPointUrl).withProduct(Product.POSTFIX).withVariable("maxVar", maxSize)
						.withVariable("queueType", queueType).build();
				logger.info("Definition is {}", def.variables.encodePrettily());
				defs.add(def);
			}
		}
		return defs;
	}
}

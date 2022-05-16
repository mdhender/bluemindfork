/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
import net.bluemind.system.api.SysConfKeys;

public class NginxConnectionsLimit extends BasicTickTemplateProvider {
	private static final Logger logger = LoggerFactory.getLogger(NginxConnectionsLimit.class);
	private static final String NAME = "nginx-connections";
	private static final int WORKER_PROCESSES = 8; // hard configured in bm-nginx (ci/conf/static/nginx.conf)

	@Override
	public String templateId() {
		return NAME;
	}

	@Override
	public InputStream content() {
		return NginxConnectionsLimit.class.getClassLoader()
				.getResourceAsStream("tickconfig/nginx-connections-limit.tick");
	}

	@Override
	public List<TemplateDefinition> createDefinitions(BmContext ctx, String endPointUrl, ItemValue<Server> server) {
		Set<Product> srvProducts = EnumSet.noneOf(Product.class);
		server.value.tags.forEach(tag -> srvProducts.addAll(Product.byTag(tag)));

		List<TemplateDefinition> defs = new ArrayList<>();
		if (srvProducts.contains(Product.NGINX)) {
			ISystemConfiguration sysConfApi = ctx.provider().instance(ISystemConfiguration.class);
			Integer nginxConnection = Integer.valueOf(
					sysConfApi.getValues().values.getOrDefault(SysConfKeys.nginx_worker_connections.name(), "1024"));
			String alertId = TickTemplateHelper.newId(Product.NGINX, NAME, server);
			int maxValue = nginxConnection * WORKER_PROCESSES * 80 / 100;
			TemplateDefinition def = new TickTemplateDefBuilder(alertId).withDatalocation(server.uid)
					.withEndPoint(endPointUrl).withProduct(Product.NGINX).withVariable("maxVar", maxValue).build();
			defs.add(def);
			logger.info("Alerting when NGINX max workers connections > {}", maxValue);
		}
		return defs;
	}

}

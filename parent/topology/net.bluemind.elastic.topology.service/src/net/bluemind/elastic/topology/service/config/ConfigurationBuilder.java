/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.elastic.topology.service.config;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.elastic.topology.service.EsTopology.NodeRole;
import net.bluemind.server.api.Server;

public class ConfigurationBuilder {

	private final ItemValue<Server> node;
	private final Configuration fmConfig;
	private Set<NodeRole> roles;
	private List<String> seeds;
	private boolean bootstrap;

	ConfigurationBuilder(ItemValue<Server> targetNode, Configuration fmConfig) {
		this.node = targetNode;
		this.fmConfig = fmConfig;
	}

	public ConfigurationBuilder bootstrapRequired() {
		this.bootstrap = true;
		return this;
	}

	public ConfigurationBuilder withSeedNodes(List<String> seeds) {
		this.seeds = seeds;
		return this;
	}

	public ConfigurationBuilder withRoles(Set<NodeRole> roles) {
		this.roles = roles;
		return this;
	}

	public EsConfiguration build() {
		try {
			Template tpl = fmConfig
					.getTemplate(bootstrap ? "clustered-node.tpl" : "clustered-node-after-bootstrap.tpl");
			StringWriter sw = new StringWriter();
			Map<String, Object> model = new HashMap<>();
			model.put("installationId", InstallationId.getIdentifier());
			model.put("serverIp", node.value.address());
			model.put("memlock", System.getProperty("elastic.config.memlock", "true"));
			model.put("roles",
					roles.stream().map(nr -> nr.name().toLowerCase()).collect(Collectors.joining(", ", "[ ", " ]")));
			model.put("otherSeedNodes", seeds.stream().filter(s -> !s.equals(node.value.address()))
					.map("\"%s\""::formatted).collect(Collectors.joining(", ", "[ ", " ]")));
			model.put("seedNodes",
					seeds.stream().map("\"%s\""::formatted).collect(Collectors.joining(", ", "[ ", " ]")));
			tpl.process(model, sw);
			return new EsConfiguration(sw.toString().getBytes());
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

}

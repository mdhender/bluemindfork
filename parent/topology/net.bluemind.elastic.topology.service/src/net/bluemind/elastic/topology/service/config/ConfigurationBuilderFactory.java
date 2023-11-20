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

import freemarker.template.Configuration;
import net.bluemind.common.freemarker.EquinoxTemplateLoader;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;

public class ConfigurationBuilderFactory {

	private static final Configuration fmConfig = loadConfig();

	private static Configuration loadConfig() {
		Configuration freemarkerCfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		freemarkerCfg.setTemplateLoader(
				new EquinoxTemplateLoader(ConfigurationBuilderFactory.class.getClassLoader(), "/configurations/"));
		freemarkerCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		return freemarkerCfg;
	}

	public static ConfigurationBuilder newBuilder(ItemValue<Server> targetNode) {
		return new ConfigurationBuilder(targetNode, fmConfig);
	}

}

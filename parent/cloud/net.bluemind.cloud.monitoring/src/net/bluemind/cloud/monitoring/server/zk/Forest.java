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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cloud.monitoring.server.zk;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import net.bluemind.central.reverse.proxy.stream.ForestInstancesLoader;

public class Forest extends ForestInstancesLoader {

	private static final Logger logger = LoggerFactory.getLogger(Forest.class);

	public Forest(Config config) {
		super(config);
	}

	public Set<ZkNode> whiteListedInstancesNode() {
		try {
			return curator.getChildren().forPath(BASE_PATH).stream() //
					.flatMap(child -> {
						try {
							return curator.getChildren().forPath(BASE_PATH + "/" + child).stream()
									.map(path -> new ZkNode(BASE_PATH, child, path));
						} catch (Exception e) {
							logger.error("Error loading content ({})", e.getMessage());
						}
						return null;
					}).collect(Collectors.toSet());
		} catch (Exception e) {
			logger.error("Error loading content ({})", e.getMessage());
			return Collections.emptySet();
		}
	}

	@Override
	public String getConfigForestId(Config config) {
		return "";
	}

}

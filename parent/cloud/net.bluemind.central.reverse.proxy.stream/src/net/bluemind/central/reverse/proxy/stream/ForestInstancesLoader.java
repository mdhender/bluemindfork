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
package net.bluemind.central.reverse.proxy.stream;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

public class ForestInstancesLoader implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ForestInstancesLoader.class);
	private final String zkBoot;
	private final CuratorFramework curator;
	private final String forestId;

	static {
		System.setProperty("zookeeper.sasl.client", "false");
	}

	public ForestInstancesLoader(Config config) {
		this.forestId = config.getString("bm.crp.stream.forest-id");
		this.zkBoot = zkBootstrap(config);
		RetryPolicy rt = new BoundedExponentialBackoffRetry(100, 10000, 15);
		this.curator = CuratorFrameworkFactory.newClient(zkBoot, rt);
		curator.start();
		try {
			curator.blockUntilConnected();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	public Set<String> whiteListedInstances() {
		try {
			List<String> children = curator.getChildren().forPath("/bluemind.net/v5/" + forestId);
			logger.info("[forest {}] WhiteListed installations are: {}", forestId, children);
			return Set.copyOf(children);
		} catch (Exception e) {
			logger.error("Error loading {} content ({})", forestId, e.getMessage());
			return Collections.emptySet();
		}

	}

	private static String zkBootstrap(Config config) {

		String zkBootstrap = config.hasPath("bm.zk.servers") ? config.getString("bm.zk.servers") : null;
		if (zkBootstrap == null) {
			File local = new File("/etc/bm/kafka.properties");
			logger.warn("Loading from legacy {}", local.getAbsolutePath());
			if (!local.exists()) {
				local = new File(System.getProperty("user.home") + "/kafka.properties");
			}
			if (local.exists()) {
				Properties tmp = new Properties();
				try (InputStream in = Files.newInputStream(local.toPath())) {
					tmp.load(in);
				} catch (Exception e) {
					logger.warn(e.getMessage());
				}
				zkBootstrap = tmp.getProperty("zookeeper.servers");
			}
		}
		return zkBootstrap;
	}

	@Override
	public void close() {
		curator.close();
	}

}
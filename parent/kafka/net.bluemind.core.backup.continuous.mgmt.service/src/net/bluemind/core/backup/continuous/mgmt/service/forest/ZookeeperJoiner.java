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
package net.bluemind.core.backup.continuous.mgmt.service.forest;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;

import net.bluemind.core.api.fault.ServerFault;

public class ZookeeperJoiner implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(ZookeeperJoiner.class);

	private final String forestId;
	private final CuratorFramework curator;

	private static final CharMatcher FOREST_ID_RULE = CharMatcher.inRange('a', 'z').or(CharMatcher.anyOf(".-_"));

	public ZookeeperJoiner(String forestId) {
		if (!FOREST_ID_RULE.matchesAllOf(forestId)) {
			throw new ServerFault("'" + forestId + "' is invalid, only a-z.-_ are allowed.");
		}
		this.forestId = forestId;
		String zkBoot = zkBootstrap();
		RetryPolicy rt = new BoundedExponentialBackoffRetry(100, 10000, 15);
		this.curator = CuratorFrameworkFactory.newClient(zkBoot, rt);
		try {
			curator.blockUntilConnected(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ServerFault("Could not connect to zookeeper " + zkBoot + " in 5sec.");
		}
	}

	@Override
	public void close() {
		curator.close();
	}

	public void join(String installationId) {
		try {
			String shortInst = installationId.replaceAll("^bluemind-", "");
			curator.createContainers("bluemind.net/" + forestId);
			curator.create().creatingParentsIfNeeded().forPath("bluemind.net/" + forestId + "/" + shortInst);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private static String zkBootstrap() {
		String zkBootstrap = System.getProperty("bm.zk.servers");
		if (zkBootstrap == null) {
			File local = new File("/etc/bm/kafka.properties");
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

}

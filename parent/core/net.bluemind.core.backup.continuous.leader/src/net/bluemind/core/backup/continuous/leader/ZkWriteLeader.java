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
package net.bluemind.core.backup.continuous.leader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatch.CloseMode;
import org.apache.curator.framework.recipes.leader.LeaderLatch.State;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.shaded.com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.DataLocation;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.api.InstallationWriteLeader;
import net.bluemind.lib.vertx.VertxPlatform;

public class ZkWriteLeader implements InstallationWriteLeader {

	private static final Logger logger = LoggerFactory.getLogger(ZkWriteLeader.class);
	private LeaderLatch latch;
	private CuratorFramework curator;
	private final CompletableFuture<Void> electionResult;
	private final String path;

	public ZkWriteLeader(boolean applyForLeadership) {
		this(zkBootstrap(), applyForLeadership);
	}

	private ZkWriteLeader(String zkBootstrap, boolean applyForLeadership) {
		RetryPolicy rt = new BoundedExponentialBackoffRetry(100, 10000, 15);
		this.curator = CuratorFrameworkFactory.newClient(zkBootstrap, rt);
		this.electionResult = new CompletableFuture<>();
		this.path = "/" + InstallationId.getIdentifier() + ".leader";
		curator.start();
		try {
			logger.info("Connecting to zk {}...", zkBootstrap);
			curator.blockUntilConnected();
			logger.info("Connected to {}", zkBootstrap);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		this.latch = new LeaderLatch(curator, path, participantId());
		latch.addListener(new LeaderLatchListener() {

			@Override
			public void notLeader() {
				logger.info("[{}] DEMOTED {}", path, latch);
				VertxPlatform.eventBus().publish("backup.write.leadership", Boolean.FALSE);
				electionResult.complete(null);
			}

			@Override
			public void isLeader() {
				logger.info("[{}] PROMOTED to leader {}", path, latch);
				VertxPlatform.eventBus().publish("backup.write.leadership", Boolean.TRUE);
				electionResult.complete(null);
			}
		});
		if (applyForLeadership) {
			logger.info("Apply for {} leadership as required.", path);
			applyForLeadership();
		}
	}

	@Override
	public void applyForLeadership() {
		if (latch.getState() == State.STARTED) {
			logger.info("[{}] latch {} already started.", path, latch);
			return;
		}

		try {
			latch.start();
			electionResult.get(20, TimeUnit.SECONDS);
			logger.info("[{}] latch {} started, leader => {}, participants: {}", path, latch, isLeader(),
					latch.getParticipants());
		} catch (TimeoutException to) {
			logger.warn("[{}] latch {} timed out, leader: {} ({})", path, latch, isLeader(), to.getMessage());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new ZkRuntimeException(e);
		}
	}

	private String participantId() {
		return DataLocation.current() + "-" //
				+ System.getProperty("net.bluemind.property.product", "unknown") + "-"//
				+ System.getProperty("zk.participant", "unknown");
	}

	@Override
	public String toString() {
		try {
			return MoreObjects.toStringHelper(ZkWriteLeader.class)//
					.add("leader", isLeader())//
					.add("participants", latch.getParticipants())//
					.add("curator", curator)//
					.toString();
		} catch (Exception e) {
			throw new ZkRuntimeException(e);
		}
	}

	@Override
	public boolean isLeader() {
		return latch.hasLeadership();
	}

	@Override
	public void releaseLeadership() {
		try {
			if (latch.getState() == State.STARTED) {
				latch.close(CloseMode.NOTIFY_LEADER);
				logger.info("[{}] latch {} closed.", path, latch);
			}
		} catch (Exception e) {
			throw new ZkRuntimeException(e);
		}
	}

	@Override
	public void close() {
		releaseLeadership();
		curator.close();
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

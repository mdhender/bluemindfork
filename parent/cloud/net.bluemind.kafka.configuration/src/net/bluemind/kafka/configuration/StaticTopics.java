/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.kafka.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class StaticTopics {

	static final String PLUGIN_ID = "net.bluemind.kafka.configuration";
	private static final String POINT_NAME = "topics";
	private static final String ELEM = "topic";

	private static final Logger logger = LoggerFactory.getLogger(StaticTopics.class);

	private StaticTopics() {

	}

	public static class KTopic {
		public String name;
		public int partitions;
		public int replicas;

		public KTopic(String n, int p, int rep) {
			this.name = n;
			this.partitions = p;
			this.replicas = rep;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(KTopic.class)//
					.add("name", name)//
					.add("partitions", partitions)//
					.add("replicas", replicas)//
					.toString();
		}
	}

	@SuppressWarnings("serial")
	public static class RuntimeFutureException extends RuntimeException {
		public RuntimeFutureException(Throwable ie) {
			super(ie);
		}
	}

	public static CompletableFuture<Void> reconfigure(AdminClient kAdm, IKafkaBroker defaults) {
		logger.info("Reconfigure kafka topic with {} and broker {}", kAdm, defaults);
		CompletableFuture<Void> result = new CompletableFuture<>();
		List<KTopic> topics = loadTopics(defaults);
		Map<String, KTopic> byName = topics.stream().collect(Collectors.toMap(t -> t.name, t -> t));
		kAdm.listTopics().names().thenApply(names -> {
			logger.info("inKafka: {}, required: {}", names, byName.keySet());
			Set<String> missingTopics = Sets.difference(byName.keySet(), names);
			List<NewTopic> toCreate = new ArrayList<>(missingTopics.size());
			for (String missing : missingTopics) {
				KTopic top = byName.get(missing);
				logger.info("Topic {} is missing.", top);
				NewTopic nt = new NewTopic(top.name, top.partitions, (short) top.replicas);
				toCreate.add(nt);
			}
			return toCreate;
		}).thenApply(toCreate -> {
			logger.info("Should create {} topics in kafka", toCreate.size());
			kAdm.createTopics(toCreate).all().whenComplete((v, ex) -> {
				if (ex == null) {
					result.complete(null);
				} else {
					logger.error(ex.getMessage(), ex);
					result.completeExceptionally(ex);
				}
			});
			return null;
		});
		return result;
	}

	private static Integer parseInt(String s) {
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException nfe) {
			return null;
		}
	}

	private static List<KTopic> loadTopics(IKafkaBroker defaults) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		Objects.requireNonNull(registry, "OSGi registry is null");
		IExtensionPoint point = registry.getExtensionPoint(PLUGIN_ID, POINT_NAME);
		if (point == null) {
			logger.error("point {}.{} not found.");
			return Collections.emptyList();
		}
		IExtension[] extensions = point.getExtensions();
		List<KTopic> topics = new ArrayList<>(extensions.length);
		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (ELEM.equals(e.getName())) {
					String name = e.getAttribute("name");
					int partitions = Optional.ofNullable(e.getAttribute("partitions")).map(StaticTopics::parseInt)
							.orElse(defaults.defaultPartitions());
					int replication = Optional.ofNullable(e.getAttribute("replicas")).map(StaticTopics::parseInt)
							.orElse(defaults.maxReplicas());
					replication = Math.min(replication, defaults.maxReplicas());
					topics.add(new KTopic(name, partitions, replication));
				}
			}
		}
		return topics;
	}

}

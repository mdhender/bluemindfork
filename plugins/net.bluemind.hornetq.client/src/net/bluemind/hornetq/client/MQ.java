/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.hornetq.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.flakeidgen.FlakeIdGenerator;

import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.impl.ClusterClient;
import net.bluemind.hornetq.client.impl.ClusterMember;
import net.bluemind.hornetq.client.impl.ClusterNode;

public final class MQ {

	private static final Logger logger = LoggerFactory.getLogger(MQ.class);

	public static final String MEMBERSHIP_EVENTS_ADDRESS = "hazelcast.membership";
	private static final Set<String> clusterMembersJvms = Sets.newHashSet("bm-core", "bm-eas", "bm-webserver",
			"unknown");

	/**
	 * For now use a fixed cluster id as we don't use multicast anymore and want to
	 * avoid the split between bluemind-noid & bluemind-<mcast.id> after install.
	 */
	public static final String CLUSTER_ID = "bluemind-72D26E8A-5BB1-48A4-BC71-EEE92E0CE4EE";

	@FunctionalInterface
	public interface IMQConnectHandler {

		void connected();

		default void connectionFailed(Throwable t) {
			logger.error("HZ connect failed", t);
		}

	}

	private static final ClusterNode nodeImpl = chooseImplementation();

	private static ClusterNode chooseImplementation() {
		String jvmType = System.getProperty("net.bluemind.property.product", "unknown");

		boolean nativeClientPossible = false;
		try {
			Class.forName("com.hazelcast.client.config.ClientConfig");
			nativeClientPossible = true;
		} catch (Exception e) {
			logger.warn("HZ native client is not possible in this JVM, client fragment missing ({})", e.getMessage());
		}
		if (clusterMembersJvms.contains(jvmType) || !nativeClientPossible) {
			logger.info("HZ cluster member implementation was chosen for {}.", jvmType);
			return new ClusterMember(jvmType);
		} else {
			logger.info("HZ native client implementation was chosen for {}.", jvmType);
			return new ClusterClient(jvmType);
		}
	}

	public static CompletableFuture<Void> init() {
		return nodeImpl.init();
	}

	public static synchronized final void init(final IMQConnectHandler handler) {
		nodeImpl.init(handler);
	}

	/**
	 * The cluster tries to keep a cluster-wide time which might be different than
	 * the member's own system time. Cluster-wide time is -almost- the same on all
	 * members of the cluster.
	 * 
	 * @return the timestamp
	 */
	public static long clusterTime() {
		return nodeImpl.clusterTime();
	}

	public static interface SharedMap<K, V> {
		void put(K k, V v);

		V get(K k);

		default void putAll(Map<K, V> map) {
			map.forEach(this::put);
		}

		Set<K> keys();

		void remove(K k);
	}

	public static <K, V> SharedMap<K, V> sharedMap(String name) {
		return nodeImpl.sharedMap(name);
	}

	/**
	 * @param topic
	 * @param handler
	 * @return
	 */
	public static Consumer registerConsumer(String topic, OutOfProcessMessageHandler handler) {
		return nodeImpl.registerConsumer(topic, null, handler);
	}

	/**
	 * @param topic
	 * @param handler
	 * @return
	 */
	public static Consumer registerConsumer(String topic, Predicate<JsonObject> filter,
			OutOfProcessMessageHandler handler) {
		return nodeImpl.registerConsumer(topic, filter, handler);
	}

	public static Producer registerProducer(String topic) {
		return nodeImpl.registerProducer(topic);
	}

	public static Producer getProducer(String topic) {
		return nodeImpl.getProducer(topic);
	}

	public static OOPMessage newMessage() {
		return new OOPMessage(new JsonObject());
	}

	public static IdGenerator newIdGenerator(String seqName) {
		FlakeIdGenerator idGen = nodeImpl.hz().join().getFlakeIdGenerator(seqName);
		return new IdGenerator(idGen);
	}

	public static List<String> topics() {
		HazelcastInstance hz = nodeImpl.hz().join();
		Collection<DistributedObject> objects = hz.getDistributedObjects();
		List<String> ret = new ArrayList<>(objects.size());
		for (DistributedObject remote : objects) {
			if ("hz:impl:reliableTopicService".equals(remote.getServiceName())) {
				ret.add(remote.getName());
			}
		}
		return ret;
	}

}

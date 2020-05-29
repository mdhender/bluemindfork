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
package net.bluemind.hornetq.client.impl;

import java.io.File;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleEvent.LifecycleState;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.LifecycleService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.BmIni;
import net.bluemind.hornetq.client.Consumer;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.lib.vertx.VertxPlatform;

public abstract class ClusterNode {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	protected final Map<String, Producer> producers = new ConcurrentHashMap<>();
	protected final CompletableFuture<HazelcastInstance> hzStart = new CompletableFuture<>();

	private final ConcurrentHashMap<String, RegisteredConsumer> consumerRegistrations = new ConcurrentHashMap<>();

	private static class RegisteredConsumer {

		private final String topic;
		private final Predicate<JsonObject> filter;
		private final OutOfProcessMessageHandler handler;

		public RegisteredConsumer(String topic, Predicate<JsonObject> filter, OutOfProcessMessageHandler handler) {
			this.topic = topic;
			this.filter = filter;
			this.handler = handler;
		}

	}

	protected ClusterNode(String jvmType) {
		logger.info("************* HZ CONNECT *************");

		Thread hzConnect = new Thread(() -> {
			try {
				HazelcastInstance hzInstance = hazelcastConnectImpl(jvmType);
				setupListener(hzInstance);
				setupMetrics(hzInstance);
				hzStart.complete(hzInstance);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				hzStart.completeExceptionally(e);
			}
		}, "bm-hz-connect");
		hzConnect.setDaemon(true);
		hzConnect.start();
	}

	public CompletableFuture<HazelcastInstance> hz() {
		return hzStart;
	}

	protected abstract void setupMetrics(HazelcastInstance hzInstance);

	private void setupListener(HazelcastInstance hz) {
		LifecycleService lifecycle = hz.getLifecycleService();
		lifecycle.addLifecycleListener(new LifecycleListener() {

			@Override
			public void stateChanged(LifecycleEvent event) {
				logger.info("HZ cluster switched to state {}, running: {}", event.getState(), lifecycle.isRunning());
				if (event.getState() == LifecycleState.CLIENT_CONNECTED && !consumerRegistrations.isEmpty()) {
					logger.info("Initiate consumer(s) refresh....");
					refreshConsumers();
				}
			}
		});
		hz.getCluster().addMembershipListener(new MembershipListener() {

			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
				Member newMember = membershipEvent.getMember();
				String memberJvm = memberJvm(newMember);
				logger.info("JVM {} {} left cluster.", memberJvm, newMember.getUuid());
				VertxPlatform.eventBus().publish(MQ.MEMBERSHIP_EVENTS_ADDRESS, new JsonObject() //
						.put("type", "memberRemoved") //
						.put("memberKind", memberJvm) //
						.put("memberUuid", newMember.getUuid())
						.put("memberAddress", newMember.getAddress().toString()));
			}

			@Override
			public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
				logger.debug("attribute changed.");
			}

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				Member newMember = membershipEvent.getMember();
				String memberJvm = memberJvm(newMember);
				logger.info("JVM {} {} joined cluster.", memberJvm, newMember.getUuid());
				VertxPlatform.eventBus().publish(MQ.MEMBERSHIP_EVENTS_ADDRESS, new JsonObject() //
						.put("type", "memberAdded") //
						.put("memberKind", memberJvm) //
						.put("memberUuid", newMember.getUuid())
						.put("memberAddress", newMember.getAddress().toString()));
			}
		});
		logger.info("Connected through {}", hz);
	}

	protected static String memberJvm(Member m) {
		return Optional.ofNullable(m.getStringAttribute("bluemind.kind")).orElse("unknown");
	}

	private synchronized void refreshConsumers() {
		hzStart.thenAccept(hz -> {
			List<RegisteredConsumer> toReAdd = new ArrayList<>(consumerRegistrations.size());
			consumerRegistrations.forEach((regId, toRegister) -> {
				hz.removeDistributedObjectListener(regId);
				toReAdd.add(toRegister);
			});
			consumerRegistrations.clear();
			int i = 0;
			for (RegisteredConsumer rc : toReAdd) {
				try {
					registerConsumer(rc.topic, rc.filter, rc.handler);
				} catch (Exception e) {
					logger.error("Failed to register consumer on {}", rc.topic, e);
					consumerRegistrations.put("failed." + (i++), rc);
				}
			}
		});
	}

	public CompletableFuture<Void> init() {
		CompletableFuture<Void> initFuture = new CompletableFuture<>();
		hzStart.whenComplete((hz, ex) -> {
			if (ex == null) {
				initFuture.complete(null);
			} else {
				initFuture.completeExceptionally(ex);
			}
		});
		return initFuture;
	}

	public synchronized final void init(final IMQConnectHandler handler) {
		logger.info("HZ setup for {}....", handler);
		hzStart.whenComplete((hz, ex) -> {
			if (ex == null) {
				handler.connected();
			} else {
				handler.connectionFailed(ex);
			}
		});
	}

	protected String memberAddress() {
		String validMemberAddress = null;
		if (new File("/etc/bm/bm.ini").exists()) {
			validMemberAddress = BmIni.value("hz-member-address");
			if (validMemberAddress == null) {
				validMemberAddress = BmIni.value("host");
			}
		}
		if (validMemberAddress == null) {
			String myIp = getMyIpAddress();
			logger.warn("Valid member address set to {}", myIp);
			validMemberAddress = myIp;
		}
		return validMemberAddress.trim();
	}

	protected abstract HazelcastInstance hazelcastConnectImpl(String jvmType);

	private static String getMyIpAddress() {
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					return ia.getAddress().getHostAddress();
				}
			}
		} catch (SocketException e) {
		}
		throw new RuntimeException("Can't figure out a suitable ipv4 address to bind on.");
	}

	public long clusterTime() {
		return hzStart.thenApply(hz -> hz.getCluster().getClusterTime()).join();
	}

	public <K, V> SharedMap<K, V> sharedMap(String name) {
		return hzStart.thenApply(hz -> {
			IMap<K, V> imap = hz.getMap(name);
			return new SharedMap<K, V>() {

				@Override
				public void put(K k, V v) {
					if (v == null) {
						imap.delete(k);
					} else {
						imap.set(k, v);
					}
				}

				@Override
				public V get(K k) {
					return imap.get(k);
				}

				public void remove(K k) {
					imap.delete(k);
				}

				@Override
				public Set<K> keys() {
					return imap.keySet();
				}
			};
		}).join();
	}

	/**
	 * @param topic
	 * @param handler
	 * @return
	 */
	public Consumer registerConsumer(String topic, OutOfProcessMessageHandler handler) {
		return registerConsumer(topic, null, handler);
	}

	/**
	 * @param topic
	 * @param handler
	 * @return
	 */
	public Consumer registerConsumer(String topic, Predicate<JsonObject> filter, OutOfProcessMessageHandler handler) {
		RegisteredConsumer rc = new RegisteredConsumer(topic, filter, handler);
		CompletableFuture<Consumer> cons = new CompletableFuture<>();
		hzStart.thenAccept(hz -> {
			try {
				ITopic<String> hzTopic = hz.getReliableTopic(topic);
				MessageListener<String> basicListener = null;
				if (filter != null) {
					basicListener = (Message<String> message) -> {
						JsonObject payload = new JsonObject(message.getMessageObject());
						if (filter.test(payload)) {
							handler.handle(new OOPMessage(payload));
						} else {
							logger.debug("Msg dropped by filter.");
						}
					};
				} else {
					basicListener = (Message<String> message) -> {
						JsonObject payload = new JsonObject(message.getMessageObject());
						handler.handle(new OOPMessage(payload));
					};
				}
				String regId = hzTopic.addMessageListener(new TopicListener(basicListener));
				consumerRegistrations.put(regId, rc);
				cons.complete(new Consumer(() -> {
					hz.removeDistributedObjectListener(regId);
					consumerRegistrations.remove(regId);
				}));
			} catch (Exception e) {
				cons.completeExceptionally(e);
			}
		});
		return cons.join();
	}

	public Producer registerProducer(String topic) {
		return getProducer(topic);
	}

	private Producer createProducerImpl(String topic) {
		CompletableFuture<Producer> prodFuture = new CompletableFuture<>();
		hzStart.thenAccept(hz -> {
			ITopic<String> hzTopic = hz.getReliableTopic(topic);
			Producer prod = new Producer(hzTopic);
			prodFuture.complete(prod);
		});
		return prodFuture.join();
	}

	public Producer getProducer(String topic) {
		return producers.computeIfAbsent(topic, t -> createProducerImpl(t));
	}

}

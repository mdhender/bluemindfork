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
package net.bluemind.forest.cloud.service;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.map.listener.EntryAddedListener;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.forest.cloud.api.ForestTopology;
import net.bluemind.forest.cloud.api.IForestJoin;
import net.bluemind.forest.cloud.api.Instance;
import net.bluemind.kafka.configuration.Brokers;
import net.bluemind.kafka.configuration.IKafkaBroker;
import net.bluemind.kafka.configuration.LocalProducer;
import net.bluemind.kafka.configuration.StaticTopics;

public class ForestJoinService implements IForestJoin {

	private static final Logger logger = LoggerFactory.getLogger(ForestJoinService.class);

	public static final IKafkaBroker storageLayer = Brokers.locate();
	public static final UUID JVM_ID = UUID.randomUUID();
	public static final String ORIGIN = "forest-cloud-" + JVM_ID;

	public static void init() {
		logger.info("Storage layer is {}", storageLayer);
		withAdminClient((kAdm, broker) -> {
			try {
				StaticTopics.reconfigure(kAdm, broker).get(1, TimeUnit.MINUTES);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.error(e.getMessage(), e);
			}
		});
	}

	public static void withAdminClient(BiConsumer<AdminClient, IKafkaBroker> needsAdmin) {
		String ip = storageLayer.inspectAddress();
		Properties properties = new Properties();
		properties.put("bootstrap.servers", ip + ":" + storageLayer.port());
		properties.put("client.id", "forest-rest-" + JVM_ID.toString());
		try (AdminClient kAdm = AdminClient.create(properties)) {
			needsAdmin.accept(kAdm, storageLayer);
		}
	}

	private final String sharedAlias;
	private final BmContext context;
	private final KafkaProducer<String, String> producer;
	private final ObjectMapper objectMapper;

	private final HazelcastInstance hz;

	public ForestJoinService(BmContext context, KafkaProducer<String, String> kafkaProducer, HazelcastInstance hz,
			String sharedAlias) {
		this.sharedAlias = sharedAlias;
		this.context = context;
		this.producer = kafkaProducer;
		this.hz = hz;
		this.objectMapper = new ObjectMapper();
		logger.debug("{} {}", this.context, this.sharedAlias);
	}

	@Override
	public ForestTopology handshake(Instance inst) {
		logger.info("handshake {}", inst);
		ForestTopology topo = new ForestTopology();
		topo.broker = ForestTopology.KafkaListener.of(storageLayer.kafkaListener());
		String instSer = null;
		try {
			instSer = objectMapper.writeValueAsString(inst);
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}

		IMap<String, String> joined = hz.getMap(KafkaTopics.JOINED);
		CompletableFuture<Void> joinedProm = new CompletableFuture<>();
		String listenerKey = joined.addEntryListener(new EntryAddedListener<String, Instance>() {

			@Override
			public void entryAdded(EntryEvent<String, Instance> event) {
				System.err.println("event " + event.getKey());
				if (event.getKey().equals(inst.installationId)) {
					joinedProm.complete(null);
				}
			}
		}, false);

		producer.send(new ProducerRecord<String, String>(KafkaTopics.JOINING, inst.installationId, instSer),
				(m, ex) -> {
					logger.info("pushed {} to JOINING {}, {}", inst.installationId, m, ex);
				});
		try {
			joinedProm.get(18, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		} finally {
			joined.removeEntryListener(listenerKey);
		}
		return topo;
	}

	public static class FJoinServiceFactory
			implements ServerSideServiceProvider.IServerSideServiceFactory<IForestJoin> {

		private final KafkaProducer<String, String> prod;

		public FJoinServiceFactory() {
			prod = LocalProducer.create(storageLayer.inspectAddress() + ":9093");
		}

		@Override
		public Class<IForestJoin> factoryClass() {
			return IForestJoin.class;
		}

		@Override
		public IForestJoin instance(BmContext context, String... params) throws ServerFault {
			if (params == null || params.length < 1) {
				throw new ServerFault("wrong number of instance parameters");
			}

			return new ForestJoinService(context, prod, Activator.getHazelcast(), params[0]);
		}

	}

}

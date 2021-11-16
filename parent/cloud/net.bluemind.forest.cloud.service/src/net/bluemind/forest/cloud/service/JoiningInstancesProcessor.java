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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.core.IMap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.forest.cloud.api.Instance;
import net.bluemind.forest.cloud.api.Instance.Partition;
import net.bluemind.forest.instance.api.ForestEnpoints;
import net.bluemind.forest.instance.api.IForestEnrollment;
import net.bluemind.forest.instance.api.IForestOrders;
import net.bluemind.forest.instance.api.ProducerSetup;
import net.bluemind.kafka.configuration.Brokers;
import net.bluemind.kafka.configuration.IKafkaBroker;
import net.bluemind.kafka.configuration.LocalConsumer;
import net.bluemind.kafka.configuration.LocalProducer;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

/**
 * Consumes in {@link KafkaTopics#JOINING}, writes to {@link KafkaTopics#JOINED}
 * if ok.
 *
 */
public class JoiningInstancesProcessor extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(JoiningInstancesProcessor.class);
	private KafkaConsumer<String, String> consumer;
	private boolean stopped;
	private KafkaProducer<String, String> producer;

	@Override
	public void start() {
		logger.info("Starting {}", this);
		IKafkaBroker broker = Brokers.locate();
		String bootstrap = broker.inspectAddress() + ":" + broker.port();
		this.consumer = LocalConsumer.create(bootstrap, "forest-joining-group",
				"processor-" + ForestJoinService.JVM_ID);
		this.producer = LocalProducer.create(bootstrap);
		consumer.subscribe(Arrays.asList(KafkaTopics.JOINING), new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				logger.info("partitionsRevoked {}", partitions);
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				logger.info("partitionsAssigned {}", partitions);
				System.err.println("Seek " + partitions + " !!!!");
				consumer.seekToBeginning(partitions);
			}
		});

		vertx.runOnContext(this::consume);

	}

	@Override
	public void stop() {
		this.stopped = true;
	}

	private void consume(@SuppressWarnings("unused") Void v) {
		ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
		for (ConsumerRecord<String, String> record : records) {
			String recValue = record.value();
			logger.info("offset = {}, key = {}, value = {}", record.offset(), record.key(), recValue);
			if (recValue == null) {
				logger.info("{} does not need joining anymore", record.key());
				continue;
			}
			try {
				Instance parsed = Mapper.get().readValue(recValue, Instance.class);
				CompletableFuture<Void> joinProm = join(parsed).whenComplete((result, ex) -> {
					if (ex == null) {
						System.err.println("storing join to hazelcast...");
						IMap<String, String> memCopy = Activator.getHazelcast().getMap(KafkaTopics.JOINED);
						memCopy.set(parsed.installationId, recValue);

						producer.send(
								new ProducerRecord<String, String>(KafkaTopics.JOINED, parsed.installationId, recValue),
								(RecordMetadata metadata, Exception exception) -> {
									logger.info("Send {} to JOINED cb {}, {}", parsed.installationId, metadata,
											exception);
								});
						producer.send(
								new ProducerRecord<String, String>(KafkaTopics.JOINING, parsed.installationId, null),
								(RecordMetadata metadata, Exception exception) -> {
									logger.info("Delete {} from JOINING cb {}, {}", parsed.installationId, metadata,
											exception);
								});
					} else {
						// requeue the message ?? wait for server restart
						logger.warn("Instance processing of {} failed. Requeue ??", parsed.installationId);
					}
				});
				joinProm.get(30, TimeUnit.SECONDS);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		// vertx style while(true)
		if (!stopped) {
			vertx.runOnContext(this::consume);
		}
	}

	private CompletableFuture<Void> join(Instance bm) {
		CompletableFuture<Void> ret = new CompletableFuture<>();

		System.err.println("Working on joining instance " + bm.installationId + "...");
		try {
			logger.info("instance {}", new JsonObject(Mapper.get().writeValueAsString(bm)).encodePrettily());
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
		ClientSideServiceProvider instanceClient = ClientSideServiceProvider.getProvider(bm.externalUrl, bm.coreToken)
				.setOrigin(ForestJoinService.ORIGIN);
		IForestEnrollment enrollApi = instanceClient.instance(IForestEnrollment.class);

		// publish endpoints to the instance
		ForestEnpoints ep = new ForestEnpoints();
		enrollApi.checkpoint(ep);
		IForestOrders controlApi = instanceClient.instance(IForestOrders.class);
		List<KafkaFuture<Void>> setup = new ArrayList<>(bm.aliases.size());
		logger.info("=================== Working on {} aliases...", bm.aliases.size());
		ForestJoinService.withAdminClient((kAdm, broker) -> {
			for (Partition domain : bm.aliases) {
				String dirTopic = bm.installationId + ".dir." + domain.domain;
				String abTopic = bm.installationId + ".addressbook.addressbook_" + domain.domain;
				NewTopic forDir = new NewTopic(dirTopic, 5, (short) broker.maxReplicas());
				NewTopic forAb = new NewTopic(abTopic, 5, (short) broker.maxReplicas());
				// if not exist
				KafkaFuture<Void> domainSetup = kAdm.createTopics(Arrays.asList(forDir, forAb)).all()
						.whenComplete((v, ex) -> {
							if (ex != null) {
								logger.error("Error creating topics {} {}", forDir, forAb, ex);
								return;
							}
							logger.info("Fresh topics created, setting up producers {} {}", dirTopic, abTopic);
							ProducerSetup dirProd = new ProducerSetup();
							dirProd.containerUid = domain.domain;
							dirProd.kafkaTopic = dirTopic;
							dirProd.broker = broker.inspectAddress() + ":" + broker.port();
							controlApi.producer(dirProd);

							ProducerSetup abProd = new ProducerSetup();
							abProd.containerUid = "addressbook_" + domain.domain;
							abProd.kafkaTopic = abTopic;
							abProd.broker = broker.inspectAddress() + ":" + broker.port();
							controlApi.producer(abProd);
						});
				setup.add(domainSetup);
				// if exist we should let instance know at which version it should start
			}
		});
		KafkaFuture.allOf(setup.toArray(new KafkaFuture[0])).whenComplete((v, ex) -> {
			if (ex == null) {
				logger.info("{} JOINED.", bm.installationId);
				ret.complete(null);
			} else {
				ret.completeExceptionally(ex);
			}
		});

		return ret;

	}

	public static class VertxFacto implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new JoiningInstancesProcessor();
		}

	}

}

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
package net.bluemind.forest.instance.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.forest.instance.api.ConsumerSetup;
import net.bluemind.forest.instance.api.IForestOrders;
import net.bluemind.forest.instance.api.ProducerSetup;
import net.bluemind.kafka.configuration.LocalConsumer;
import net.bluemind.kafka.configuration.LocalProducer;

public class ForestOrdersService implements IForestOrders {

	private static final Logger logger = LoggerFactory.getLogger(ForestOrdersService.class);

	private final BmContext context;

	public ForestOrdersService(BmContext context) {
		this.context = context;
		logger.debug("{}", this.context);
	}

	@Override
	public void producer(ProducerSetup ps) {
		logger.info("Producer... {}", ps);
		KafkaProducer<String, String> producer = LocalProducer.create(ps.broker);
		long containerVersion = lastContainerVersionInTopic(ps);
		System.err.println("********* PRODUCE FOR " + ps.kafkaTopic);
		IContainers contApi = context.provider().instance(IContainers.class);
		try {
			ContainerDescriptor desc = contApi.get(ps.containerUid);
			logger.info("Start producing to {} at container version {}, type {}", ps.kafkaTopic, containerVersion,
					desc.type);

			switch (desc.type) {
			case "dir":
				System.err.println("Produce for dir");
				IChangesetAndLoad<DirEntry> cs = wrapDir(context.provider().instance(IDirectory.class, desc.domainUid));
				produceDelta(producer, ps, cs, containerVersion);
				break;
			default:
				System.err.println("Don't know how to produce data to kafka for type " + desc.type);
				break;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Store the last produced version somewhere or use kafka streams
	 * 
	 * @param ps
	 * @return
	 */
	private long lastContainerVersionInTopic(ProducerSetup ps) {
		long containerVersion = 0;
		String unique = UUID.randomUUID().toString();
		// we are a unique consumer, alone in its group
		KafkaConsumer<String, String> consumer = LocalConsumer.create(ps.broker, "pre-produce-" + unique, unique);
		List<String> toSub = Arrays.asList(ps.kafkaTopic);
		logger.info("Subscribing to {}", toSub);
		consumer.subscribe(toSub, new ConsumerRebalanceListener() {

			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
			}

			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
				logger.info("Got partitions {}", partitions);
			}
		});
		consumer.poll(Duration.ZERO);
		Set<TopicPartition> partitions = consumer.assignment();
		logger.info("Current partitions {}", partitions);

		Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitions);
		logger.info("end offsets {}", endOffsets);
		Entry<TopicPartition, Long> mostRecent = null;
		for (Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
			logger.info("Checking {}", entry);
			if (mostRecent == null || entry.getValue() > mostRecent.getValue()) {
				mostRecent = entry;
			}
		}
		logger.info("Most recent: {}", mostRecent);
		if (mostRecent != null) {
			consumer.seek(mostRecent.getKey(), mostRecent.getValue() - 1);
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
			logger.info("Got records: {}", records.count());
			for (ConsumerRecord<String, String> record : records) {
				String payload = record.value();
				if (payload.charAt(0) == '{') {
					ObjectMapper mapper = new ObjectMapper();
					try {
						JsonNode parsedJson = mapper.readTree(payload);
						// we should have an ItemValue<Something> in there
						containerVersion = parsedJson.get("version").asLong(0);
						logger.info("Container version from last item is {}", containerVersion);
					} catch (IOException e) {
						logger.error("value {}: {}", payload, e.getMessage(), e);
					}
				} else {
					// version number of a deletion
					containerVersion = Long.parseLong(payload);
				}
			}
		}
		return containerVersion;
	}

	private IChangesetAndLoad<DirEntry> wrapDir(IDirectory instance) {
		return new IChangesetAndLoad<DirEntry>() {

			@Override
			public ContainerChangeset<String> changeset(Long since) throws ServerFault {
				return instance.changeset(since);
			}

			@Override
			public List<ItemValue<DirEntry>> fetchMultiple(List<String> uids) {
				return instance.getMultiple(uids);
			}
		};
	}

	private <T> void produceDelta(KafkaProducer<String, String> p, ProducerSetup ps, IChangesetAndLoad<T> support,
			long containerVersion) {
		ContainerChangeset<String> diff = support.changeset(containerVersion);
		String delValue = Long.toString(diff.version);
		for (String del : diff.deleted) {
			p.send(new ProducerRecord<String, String>(ps.kafkaTopic, del, delValue));
		}
		for (List<String> slice : Lists.partition(diff.created, 100)) {
			List<ItemValue<T>> loaded = support.fetchMultiple(slice);
			for (ItemValue<T> entry : loaded) {
				p.send(new ProducerRecord<String, String>(ps.kafkaTopic, entry.uid, JsonUtils.asString(entry)));
			}
		}
		for (List<String> slice : Lists.partition(diff.updated, 100)) {
			List<ItemValue<T>> loaded = support.fetchMultiple(slice);
			for (ItemValue<T> entry : loaded) {
				p.send(new ProducerRecord<String, String>(ps.kafkaTopic, entry.uid, JsonUtils.asString(entry)));
			}
		}
	}

	@Override
	public void consumer(ConsumerSetup cs) {
		logger.info("Consumer... {}", cs);
	}

	public static class FOrdersServiceFactory
			implements ServerSideServiceProvider.IServerSideServiceFactory<IForestOrders> {

		@Override
		public Class<IForestOrders> factoryClass() {
			return IForestOrders.class;
		}

		@Override
		public IForestOrders instance(BmContext context, String... params) throws ServerFault {
			return new ForestOrdersService(context);
		}

	}

}

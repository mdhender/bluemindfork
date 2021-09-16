package net.bluemind.central.reverse.proxy.kafka.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import net.bluemind.central.reverse.proxy.kafka.KafkaAdminClient;

public class KafkaAdminClientImpl implements KafkaAdminClient {
	private static final Logger logger = LoggerFactory.getLogger(KafkaAdminClientImpl.class);

	private final AdminClient adminClient;

	public KafkaAdminClientImpl(String bootstrapServers) {
		Properties props = new Properties();
		props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		this.adminClient = AdminClient.create(props);
		logger.info("Created with {}: {}", bootstrapServers, adminClient);
	}

	@Override
	public Future<Set<String>> listTopics() {
		Promise<Set<String>> promise = Promise.promise();
		adminClient.listTopics().names().whenComplete((result, t) -> {
			if (Objects.isNull(t)) {
				promise.complete(result);
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Map<String, TopicDescription>> describeTopics(Collection<String> topicNames) {
		Promise<Map<String, TopicDescription>> promise = Promise.promise();
		adminClient.describeTopics(topicNames).all().whenComplete((result, t) -> {
			if (Objects.isNull(t)) {
				promise.complete(result);
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Map<String, ConsumerGroupDescription>> describeConsumerGroups(Collection<String> names) {
		Promise<Map<String, ConsumerGroupDescription>> promise = Promise.promise();
		adminClient.describeConsumerGroups(names).all().whenComplete((result, t) -> {
			if (Objects.isNull(t)) {
				promise.complete(result);
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Void> deleteConsumerGroupOffsets(String groupId, Set<TopicPartition> topicPartitions) {
		Promise<Void> promise = Promise.promise();
		adminClient.deleteConsumerGroupOffsets(groupId, topicPartitions).all().whenComplete((v, t) -> {
			if (Objects.isNull(t)) {
				promise.complete();
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Collection<ConsumerGroupListing>> listConsumerGroups() {
		Promise<Collection<ConsumerGroupListing>> promise = Promise.promise();
		adminClient.listConsumerGroups().all().whenComplete((listings, t) -> {
			if (Objects.isNull(t)) {
				promise.complete(listings);
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Collection<ConsumerGroupListing>> deleteConsumerGroups(Collection<String> groupIds) {
		Promise<Collection<ConsumerGroupListing>> promise = Promise.promise();
		adminClient.deleteConsumerGroups(groupIds).all().whenComplete((v, t) -> {
			if (Objects.isNull(t)) {
				promise.complete();
			} else {
				promise.fail(t);
			}
		});
		return promise.future();
	}

	@Override
	public Future<Void> resetTopicOffset(String consumerGroupToBeReset, Collection<String> topicNames) {
		Promise<Void> promise = Promise.promise();
		listConsumerGroups()
				.map(listings -> listings.stream().anyMatch(l -> consumerGroupToBeReset.equals(l.groupId())))
				.flatMap(exists -> Boolean.TRUE.equals(exists)
						? deleteConsumerGroups(Collections.singleton(consumerGroupToBeReset))
						: Future.succeededFuture())
				.onSuccess(v -> promise.complete()).onFailure(t -> promise.fail(t));

		return promise.future();
	}

}

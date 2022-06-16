package net.bluemind.central.reverse.proxy.model.common.kafka;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;

import io.vertx.core.Future;
import net.bluemind.central.reverse.proxy.model.common.kafka.impl.KafkaAdminClientImpl;

public interface KafkaAdminClient {

	public static KafkaAdminClient create(String bootstrapServers) {
		return new KafkaAdminClientImpl(bootstrapServers);
	}

	Future<Set<String>> listTopics();

	Future<Map<String, TopicDescription>> describeTopics(Collection<String> topicNames);

	Future<Collection<ConsumerGroupListing>> listConsumerGroups();

	Future<Map<String, ConsumerGroupDescription>> describeConsumerGroups(Collection<String> names);

	Future<Void> deleteConsumerGroupOffsets(String groupId, Set<TopicPartition> topicPartitions);

	Future<Collection<ConsumerGroupListing>> deleteConsumerGroups(Collection<String> groupIds);

	Future<Void> resetTopicOffset(String consumerGroupToBeReset, Collection<String> topicNames);

	Future<Uuid> createTopic(NewTopic newTopic, CreateTopicsOptions options);

}

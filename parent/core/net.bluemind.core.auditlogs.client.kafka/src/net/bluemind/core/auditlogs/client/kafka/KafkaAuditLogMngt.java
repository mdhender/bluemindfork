package net.bluemind.core.auditlogs.client.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.client.kafka.config.AuditLogKafkaConfig;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;
import net.bluemind.core.backup.store.kafka.config.KafkaStoreConfig;

public class KafkaAuditLogMngt implements IAuditLogMgmt {

	private static final Logger logger = LoggerFactory.getLogger(KafkaAuditLogMngt.class);
	private static final int PARTITION_COUNT = KafkaStoreConfig.get().getInt("kafka.topic.partitionCount");
	private static final short REPL_FACTOR = (short) KafkaStoreConfig.get().getInt("kafka.topic.replicationFactor");

	private static final AtomicInteger cidAlloc = new AtomicInteger();

	private final Supplier<AdminClient> adminClient;

	public KafkaAuditLogMngt(String bootstrap) {
		this.adminClient = () -> {
			Properties properties = new Properties();
			properties.put("bootstrap.servers", bootstrap);
			String cid = jvm() + "_" + InstallationId.getIdentifier() + "_" + cidAlloc.incrementAndGet();
			properties.put("client.id", cid);

			return AdminClient.create(properties);
		};
	}

	private String jvm() {
		return System.getProperty("net.bluemind.property.product", "unknown");
	}

	@Override
	public void setupAuditLogBackingStore(String domainUid) throws AuditLogCreationException {
		if (!hasKafkaTopicForDomainUid(domainUid)) {
			createKafkaTopic(AuditLogKafkaConfig.getTopic(domainUid));
		}
	}

	@Override
	public void removeAuditLogBackingStores() {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAuditLogBackingStore(String domainUid) {
		if (hasKafkaTopicForDomainUid(domainUid)) {
			removeKafkaTopic(AuditLogKafkaConfig.getTopic(domainUid));
		}

	}

	@Override
	public boolean hasAuditLogBackingStore(String domainUid) {
		return hasKafkaTopicForDomainUid(domainUid);
	}

	public void createKafkaTopic(String name) {
		try (var ac = adminClient.get()) {
			Config conf = KafkaStoreConfig.get();
			NewTopic nt = new NewTopic(name, PARTITION_COUNT, REPL_FACTOR);

			nt.configs(Map.of(//
					TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, Integer.toString(conf.getInt("kafka.topic.minIsr")), //
					TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
					Long.toString((long) (conf.getMemorySize("kafka.producer.maxRecordSize").toBytes() * 1.05)), //
					TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE, //
					TopicConfig.SEGMENT_MS_CONFIG, AuditLogKafkaConfig.getSegmentTimeMs(), //
					TopicConfig.SEGMENT_BYTES_CONFIG, AuditLogKafkaConfig.getSegmentSizeByte(), //
					TopicConfig.RETENTION_MS_CONFIG, AuditLogKafkaConfig.getRetentionTimeMs() //
			));
			CreateTopicsOptions cto = new CreateTopicsOptions();

			CreateTopicsResult res = ac.createTopics(Arrays.asList(nt), cto);
			Uuid created = res.topicId(name).get();
			logger.info("Created auditlog topic {}: {}", name, created);
		} catch (ExecutionException ex) {
			if (!(ex.getCause() instanceof TopicExistsException)) {
				throw new ServerFault(ex);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private void removeKafkaTopic(String name) {
		try (var ac = adminClient.get()) {
			ac.deleteTopics(List.of(name));
			logger.info("Delete auditlog topic '{}'", name);
		}
	}

	public boolean hasKafkaTopicForDomainUid(String domainUid) {
		String topic = AuditLogKafkaConfig.getTopic(domainUid);
		ListTopicsOptions opts = new ListTopicsOptions();
		opts.listInternal(false);
		try (var ac = adminClient.get()) {
			Map<String, TopicListing> existing = ac.listTopics(opts).namesToListings().get();
			return existing.containsKey(topic);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.error("Problem when fetching Kafka auditlog topic '{}': {}", topic, e.getMessage());
		}
		return false;
	}

}

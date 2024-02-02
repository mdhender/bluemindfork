package net.bluemind.core.auditlogs.client.kafka.config;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.auditlogs.client.loader.config.AuditLogConfig;

public class AuditLogKafkaConfig extends AuditLogConfig {
	private static final Logger logger = LoggerFactory.getLogger(AuditLogKafkaConfig.class);
	private static final String AUDIT_LOG_TOPIC_PATTERN = InstallationId.getIdentifier() + "-%s_audit";

	private static final Config INSTANCE = loadConfig();

	private AuditLogKafkaConfig() {
		super();
	}

	public static class KafkaStore {
		private KafkaStore() {

		}

		public static final String SEGMENT_TTL = "auditlog.kafka.segment_ttl";
		public static final String SEGMENT_ROLL_SIZE = "auditlog.kafka.segment_roll_size";
		public static final String AUDIT_TTL = "auditlog.kafka.audit_ttl";
	}

	public static String getTopic(String domainUid) {
		return String.format(AUDIT_LOG_TOPIC_PATTERN, domainUid);
	}

	private static Config loadConfig() {
		Config conf = ConfigFactory.load(AuditLogKafkaConfig.class.getClassLoader(), "resources/auditlog-kafka.conf");
		try {
			File local = new File("/etc/bm/auditlog-store.conf"); // NOSONAR
			if (local.exists()) {
				Config parsed = ConfigFactory.parseFile(local);
				conf = parsed.withFallback(conf);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return conf.withFallback(AuditLogConfig.get());
	}

	public static Config get() {
		return INSTANCE;
	}

	public static String getSegmentTimeMs() {
		try {
			return Long.toString(AuditLogKafkaConfig.get().getDuration(KafkaStore.SEGMENT_TTL, TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			return "86400000";
		}
	}

	public static String getSegmentSizeByte() {
		try {
			return Long.toString(AuditLogKafkaConfig.get().getMemorySize(KafkaStore.SEGMENT_ROLL_SIZE).toBytes());
		} catch (Exception e) {
			return "259200000";
		}
	}

	public static String getRetentionTimeMs() {
		try {
			return Long.toString(AuditLogKafkaConfig.get().getDuration(KafkaStore.AUDIT_TTL, TimeUnit.MILLISECONDS));
		} catch (Exception e) {
			return "128000000";
		}
	}

}

package net.bluemind.central.reverse.proxy.common.config;

import java.io.File;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class CrpConfig {

	private static final Logger logger = LoggerFactory.getLogger(CrpConfig.class);

	private static final String APPLICATION_RESOURCE = "resources/application.conf";
	private static final String VERTICLE_RESOURCE = "resources/reference.conf";
	private static final String KAFKA_CONFIG = "/etc/bm/kafka.properties";
	private static final String OVERRIDE_PATH = "/etc/bm/crp.conf";

	public static class Kafka {
		private Kafka() {

		}

		public static final String BOOTSTRAP_SERVERS = "bm.kafka.bootstrap.servers";
	}

	public static class Topic {
		private Topic() {

		}

		public static final String NAME_SUFFIX = "bm.crp.topic.name-suffix";
		public static final String PARTITION_COUNT = "bm.crp.topic.partition-count";
		public static final String REPLICATION_FACTOR = "bm.crp.topic.replication-factor";
		public static final String COMPRESSION_TYPE = "bm.crp.topic.compression-type";
		public static final String CLEANUP_POLICY = "bm.crp.topic.cleanup-policy";
		public static final String MAX_COMPACTION_LAG_MS = "bm.crp.topic.max-compaction-lag-ms";
	}

	public static class Proxy {
		private Proxy() {

		}

		public static final String PORT = "bm.crp.proxy.port";
		public static final String KEEP_ALIVE = "bm.crp.proxy.keep-alive";
		public static final String TCP_KEEP_ALIVE = "bm.crp.proxy.tcp-keep-alive";
		public static final String TCP_NO_DELAY = "bm.crp.proxy.tcp-no-delay";
		public static final String MAX_POOL_SIZE = "bm.crp.proxy.max-pool-size";
		public static final String MAX_WEB_SOCKETS = "bm.crp.proxy.max-web-sockets";
		public static final String INITIAL_CAPACITY = "bm.crp.proxy.sessions.initial-capacity";

		public static class Ssl {
			private Ssl() {

			}

			public enum Engine {
				OPEN_SSL, JDK_SSL
			}

			public static final String ACTIVE = "bm.crp.proxy.ssl.active";
			public static final String ENGINE = "bm.crp.proxy.ssl.engine";
			public static final String USE_ALPN = "bm.crp.proxy.ssl.use-alpn";
			public static final String VERIFY_HOST = "bm.crp.proxy.ssl.verify-host";
			public static final String TRUST_ALL = "bm.crp.proxy.ssl.trust-all";
		}
	}

	public static class Model {
		private Model() {

		}

		public static final String CONSUMER_GROUP_PREFIX = "bm.crp.model.consumer-group-prefix";
		public static final String CLIENT_ID_PREFIX = "bm.crp.model.client-id-prefix";
		public static final String NUMBER_OF_CONSUMER = "bm.crp.model.number-of-consumer";
	}

	public static class Stream {
		private Stream() {

		}

		public static final String ENFORCE_FOREST = "bm.crp.stream.enforce-forest";
		public static final String FOREST_ID = "bm.crp.stream.forest-id";
		public static final String APPLICATION_ID = "bm.crp.stream.application-id";
		public static final String NUMBER_OF_THREADS = "bm.crp.stream.number-of-threads";
	}

	public static class PostfixMaps {
		private PostfixMaps() {
		}

		public static final String PORT = "bm.crp.postfixmaps.port";
	}

	public static class Milter {
		private Milter() {
		}

		public static final String PORT = "bm.crp.milter.port";
		public static final String REMOTE_PORT = "bm.crp.milter.remotePort";
	}

	private static Config config;

	private CrpConfig() {
	}

	public static Config get(String name, ClassLoader loader) {
		if (Objects.isNull(config)) {
			config = applicationConfig();
		}
		Config referenceConfig = ConfigFactory.parseResourcesAnySyntax(loader, VERTICLE_RESOURCE);
		Config resolvedConfig = config.withFallback(referenceConfig).resolve();
		logger.info("{} config: {}", name, resolvedConfig.getConfig("bm.crp").root());
		return resolvedConfig;
	}

	private static Config applicationConfig() {
		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		Config bundleConfig = ConfigFactory.load(CrpConfig.class.getClassLoader(), APPLICATION_RESOURCE);
		Config applicationConfig = systemPropertyConfig.withFallback(bundleConfig);

		if (!applicationConfig.hasPath(CrpConfig.Kafka.BOOTSTRAP_SERVERS)) {
			applicationConfig = applicationConfig.withFallback(kafkaBootstrapServersConfig());
		}

		File override = new File(OVERRIDE_PATH);
		if (override.exists()) {
			try {
				Config fromDisk = ConfigFactory.parseFile(override);
				applicationConfig = fromDisk.withFallback(applicationConfig);
			} catch (ConfigException e) {
				logger.error("Invalid crp configuration override '{}', ignored: {}", override, e.getMessage());
			}
		}

		return applicationConfig;
	}

	private static Config kafkaBootstrapServersConfig() {
		File properties = new File(KAFKA_CONFIG);
		if (!properties.exists()) {
			properties = new File(System.getProperty("user.home") + "/kafka.properties");
		}
		Config parseFile = ConfigFactory.parseFile(properties);
		if (parseFile.hasPath("bootstrap.servers")) {
			String bootstrapServers = parseFile.getString("bootstrap.servers");
			ConfigValue bootstrapServer = ConfigValueFactory.fromAnyRef(bootstrapServers);
			return ConfigFactory.empty().withValue(CrpConfig.Kafka.BOOTSTRAP_SERVERS, bootstrapServer);
		}

		throw new RuntimeException("No configuration available for kafka bootstrap servers");
	}
}

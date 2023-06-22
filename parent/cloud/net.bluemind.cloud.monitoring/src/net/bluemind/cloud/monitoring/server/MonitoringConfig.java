package net.bluemind.cloud.monitoring.server;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class MonitoringConfig {

	private static final Logger logger = LoggerFactory.getLogger(MonitoringConfig.class);
	private static final Config INSTANCE = loadConfig();

	private static final String MONITORING_RESOURCE = "resources/monitoring.conf";
	private static final String KAFKA_CONFIG = "/etc/bm/kafka.properties";

	public static class Monitoring {
		private Monitoring() {

		}

		public static final String PORT = "bm.monitoring.port";
	}

	public static class Kafka {
		private Kafka() {

		}

		public static final String BOOTSTRAP_SERVERS = "bm.kafka.bootstrap.servers";
	}

	public static class Stream {
		private Stream() {

		}

		public static final String APPLICATION_ID = "bm.monitoring.application-id";
	}

	private MonitoringConfig() {

	}

	public static Config get() {
		return INSTANCE;
	}

	private static Config loadConfig() {
		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		Config bundleConfig = ConfigFactory.load(MonitoringConfig.class.getClassLoader(), MONITORING_RESOURCE);
		Config applicationConfig = systemPropertyConfig.withFallback(bundleConfig);

		if (!applicationConfig.hasPath(MonitoringConfig.Kafka.BOOTSTRAP_SERVERS)) {
			applicationConfig = applicationConfig.withFallback(kafkaBootstrapServersConfig());
		}

		logger.info("Monitoring config: {}", applicationConfig.getConfig("bm.monitoring").root());
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
			return ConfigFactory.empty().withValue(MonitoringConfig.Kafka.BOOTSTRAP_SERVERS, bootstrapServer);
		}

		throw new RuntimeException("No configuration available for kafka bootstrap servers");
	}

}

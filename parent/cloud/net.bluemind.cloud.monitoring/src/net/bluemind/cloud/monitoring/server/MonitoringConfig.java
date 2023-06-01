package net.bluemind.cloud.monitoring.server;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;

public class MonitoringConfig {

	private static final Logger logger = LoggerFactory.getLogger(MonitoringConfig.class);

	private static final String APPLICATION_RESOURCE = "resources/application.conf";
	private static final String VERTICLE_RESOURCE = "resources/reference.conf";

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

		public static final String APPLICATION_ID = "bm.stream.application-id";
	}

	private static Config config;

	private MonitoringConfig() {

	}

	public static Config get(String name, ClassLoader loader) {
		if (Objects.isNull(config)) {
			config = applicationConfig();
		}
		Config referenceConfig = ConfigFactory.parseResourcesAnySyntax(loader, VERTICLE_RESOURCE);
		Config resolvedConfig = config.withFallback(referenceConfig).resolve();
		logger.info("{} config: {}", name, resolvedConfig.getConfig("bm.monitoring").root());

		if (!resolvedConfig.hasPath("bm.kafka.bootstrap.servers")) {
			ConfigValue kafkaBootstrapServers = kafkaBootstrapServers()
					.orElseThrow(() -> new RuntimeException("No configuration available for kafka bootstrap servers"));
			resolvedConfig = resolvedConfig.withValue("bm.kafka.bootstrap.servers", kafkaBootstrapServers);
		}

		return resolvedConfig;
	}

	private static Config applicationConfig() {
		Config systemPropertyConfig = ConfigFactory.defaultApplication();
		Config bundleConfig = ConfigFactory.load(MonitoringConfig.class.getClassLoader(), APPLICATION_RESOURCE);

		return systemPropertyConfig.withFallback(bundleConfig);

	}

	@Deprecated
	private static Optional<ConfigValue> kafkaBootstrapServers() {
		File properties = new File("/etc/bm/kafka.properties");
		String originDescription = "value from /etc/bm/kafka.properties";
		if (!properties.exists()) {
			properties = new File(System.getProperty("user.home") + "/kafka.properties");
			originDescription = "value from ~/kafka.properties";
		}

		return (properties.exists()) ? kafkaBootstrapServers(properties, originDescription) : Optional.empty();
	}

	@Deprecated
	private static Optional<ConfigValue> kafkaBootstrapServers(File properties, String originDescription) {
		Properties tmp = new Properties();
		try (InputStream in = Files.newInputStream(properties.toPath())) {
			tmp.load(in);
			String bootstrapServers = tmp.getProperty("bootstrap.servers");
			ConfigValue bootstrapServer = ConfigValueFactory.fromAnyRef(bootstrapServers, originDescription);
			return Optional.of(bootstrapServer);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			return Optional.empty();
		}
	}

}

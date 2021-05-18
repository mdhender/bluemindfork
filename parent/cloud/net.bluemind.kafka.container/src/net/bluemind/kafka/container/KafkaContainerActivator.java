package net.bluemind.kafka.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;

public class KafkaContainerActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		KafkaContainerActivator.context = bundleContext;
		setupDockerEnv();
	}

	private void setupDockerEnv() {
		System.getenv().entrySet().stream().filter(e -> e.getKey().toLowerCase().startsWith("dock")).forEach(e -> {
			System.err.println(e);
		});
		Path tgtFile = Paths.get(System.getProperty("user.home"), ".testcontainers.properties");
		try {
			Files.write(tgtFile,
					"docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy\n"
							.getBytes(),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			System.err.println(tgtFile + " written.");
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		try (InputStream in = Files
				.newInputStream(Paths.get(System.getProperty("user.home"), ".docker.io.properties"))) {
			Properties p = new Properties();
			p.load(in);
			for (Object k : p.keySet()) {
				String v = p.getProperty(k.toString());
				System.err.println("[docker.io.properties] " + k + " => " + v);
			}
			String url = p.getProperty("docker.io.url");
			System.setProperty("DOCKER_HOST", url.replace("http://", "tcp://"));

			List<DockerClientProviderStrategy> configurationStrategies = new ArrayList<>();
			ServiceLoader.load(DockerClientProviderStrategy.class).forEach(configurationStrategies::add);

			for (DockerClientProviderStrategy strat : configurationStrategies) {
				System.err.println("strat: " + strat + ", avail: " + strat.getDescription());
			}
			System.err.println("just valid follows");
			DockerClientProviderStrategy valid = DockerClientProviderStrategy
					.getFirstValidStrategy(configurationStrategies);
			System.err.println("valid: " + valid);

		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	public void stop(BundleContext bundleContext) throws Exception {
		KafkaContainerActivator.context = null;
	}

}

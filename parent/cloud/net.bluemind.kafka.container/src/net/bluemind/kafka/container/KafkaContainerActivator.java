package net.bluemind.kafka.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
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
		Path tgtFile = Paths.get(System.getProperty("user.home"), ".testcontainers.properties");

		String dockerHost = "http://127.0.0.1:10000";
		String certsDir = "";

		try (InputStream in = Files
				.newInputStream(Paths.get(System.getProperty("user.home"), ".docker.io.properties"))) {
			Properties p = new Properties();
			p.load(in);
			dockerHost = p.getProperty("docker.io.url", "http://127.0.0.1:10000");
			certsDir = p.getProperty("certs", "");
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		try {
			Files.write(tgtFile,
					MessageFormat
							.format("docker.host={0}\ndocker.cert.path={1}\n",
									dockerHost.replace("http://", "tcp://").replace("https://", "tcp://"), certsDir)
							.getBytes(),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			System.err.println(tgtFile + " written.");
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		List<DockerClientProviderStrategy> configurationStrategies = new ArrayList<>();
		ServiceLoader.load(DockerClientProviderStrategy.class).forEach(configurationStrategies::add);

		for (DockerClientProviderStrategy strat : configurationStrategies) {
			System.err.println("strat: " + strat + ", avail: " + strat.getDescription());
		}
		System.err.println("just valid follows");
		DockerClientProviderStrategy valid = DockerClientProviderStrategy
				.getFirstValidStrategy(configurationStrategies);
		System.err.println("valid: " + valid);

	}

	public void stop(BundleContext bundleContext) throws Exception {
		KafkaContainerActivator.context = null;
	}

}

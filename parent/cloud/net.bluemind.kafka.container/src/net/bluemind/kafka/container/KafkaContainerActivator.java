package net.bluemind.kafka.container;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

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
		try (InputStream in = Files
				.newInputStream(Paths.get(System.getProperty("user.home"), ".docker.io.properties"))) {
			Properties p = new Properties();
			p.load(in);
			String url = p.getProperty("docker.io.url");
			System.setProperty("DOCKER_HOST", url.replace("http://", "tcp://"));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	public void stop(BundleContext bundleContext) throws Exception {
		KafkaContainerActivator.context = null;
	}

}

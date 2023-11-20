/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.elastic.topology.service.tests;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;

import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;

public class ElasticNode extends GenericContainer<ElasticNode> {
	static {
		setupDockerEnv();
	}

	private static void setupDockerEnv() {
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
		DockerClientProviderStrategy valid = DockerClientProviderStrategy
				.getFirstValidStrategy(configurationStrategies);
		System.err.println("valid: " + valid);

	}

	public ElasticNode() {
		super("docker.bluemind.net/bluemind/elasticsearch-tests:5.0");
		withExposedPorts(9200, 9300, 8021);
		withReuse(false);
		waitingFor(new org.testcontainers.containers.wait.strategy.AbstractWaitStrategy() {

			@Override
			protected void waitUntilReady() {
				NetworkHelper nh = new NetworkHelper(inspectAddress());
				System.err.println("Waiting for " + inspectAddress() + ":" + 9200);
				nh.waitForListeningPort(9200, 30, TimeUnit.SECONDS);
				nh.waitForListeningPort(9300, 30, TimeUnit.SECONDS);
				nh.waitForListeningPort(8021, 30, TimeUnit.SECONDS);
				INodeClient nc = NodeActivator.get(inspectAddress());
				InputStream input = ElasticNode.class.getClassLoader().getResourceAsStream("scripts/restart-es.sh");
				nc.writeFile("/restart-es.sh", input);
				NCUtils.exec(nc, "chmod +x /restart-es.sh");
				System.err.println("/restart-es.sh overriden.");
			}

		});
	}

	public String inspectAddress() {
		return getContainerInfo().getNetworkSettings().getNetworks().get("bridge").getIpAddress();
	}

	@Override
	public void stop() {
		String ip = inspectAddress();
		NodeActivator.forget(ip);
		DockerClient client = getDockerClient();
		var contId = getContainerId();
		super.stop();

		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
			try {
				InspectContainerResponse afterStop = client.inspectContainerCmd(contId).exec();
				System.err.println("afterStop: " + afterStop);
				return false;
			} catch (Exception e) {
				return true;
			}
		});

	}

	public void stopElastic() {
		String ip = inspectAddress();
		INodeClient nc = NodeActivator.get(ip);
		System.err.println("Stop in docker " + ip);
		ExitList res = NCUtils.exec(nc, "pkill -F /var/spool/bm-elasticsearch/data/es.pid", 2, TimeUnit.MINUTES);
		System.err.println("Stop result: " + res.getExitCode());
		ExitList clearLock = NCUtils.exec(nc, "find /var/spool/bm-elasticsearch/data/ -type f -name node.lock -delete",
				2, TimeUnit.MINUTES);
		System.err.println("Clear lock: " + clearLock.getExitCode());

	}

	public void restartElastic() {
		String ip = inspectAddress();
		INodeClient nc = NodeActivator.get(ip);
		System.err.println("Restarting in docker " + ip + " from " + Thread.currentThread().getName());
		int exitCode = NCUtils.exec(nc, "/restart-es.sh", 2, TimeUnit.MINUTES).getExitCode();
		try {
			new NetworkHelper(ip).waitForListeningPort(9300, 30, TimeUnit.SECONDS);
			System.err.println(ip + ":9300 is ok, restart existed with code " + exitCode);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				Thread.currentThread().join();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}

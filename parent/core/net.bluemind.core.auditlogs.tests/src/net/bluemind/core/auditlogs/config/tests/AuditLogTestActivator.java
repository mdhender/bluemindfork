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

package net.bluemind.core.auditlogs.config.tests;

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

public class AuditLogTestActivator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		AuditLogTestActivator.context = bundleContext;
		setupDockerEnv();

	}

	@Override
	public void stop(BundleContext arg0) throws Exception {
		AuditLogTestActivator.context = null;

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

}

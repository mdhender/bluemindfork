/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.systemcheck.checks;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.InstallationVersion;

public class ServerConformityCheck extends AbstractCheck {

	private static final String nodePluginsFolder = "/usr/share/bm-node/plugins";
	private static final String nodeJarName = "net.bluemind.node.server_";

	private static final Logger logger = LoggerFactory.getLogger(ServerConformityCheck.class);

	@Override
	public boolean canCheckWithVersion(InstallationVersion version) {
		return version.databaseVersion.startsWith("4.");
	}

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		List<ItemValue<Server>> servers = provider.instance(IServer.class, "default").allComplete();

		String targetFile = new File(nodePluginsFolder).list((folder, name) -> name.startsWith(nodeJarName))[0];
		String targetVersion = getVersion(targetFile);

		for (ItemValue<Server> server : servers) {
			try {
				logger.info("Verifying server {} packages. Target package version is {}", server.value.ip,
						targetVersion);
				INodeClient node = NodeActivator.get(server.value.address());
				Optional<FileDescription> nodeServerJar = node.listFiles(nodePluginsFolder, "jar").stream()
						.filter(f -> f.getName().startsWith(nodeJarName)).findAny();
				if (nodeServerJar.isPresent()) {
					String version = getVersion(nodeServerJar.get().getName());
					logger.info("Server package version is {}", version);
					if (!version.equals(targetVersion)) {
						logger.warn("Server {} contains incompatible package versions. Expected: {}, Actual:{}",
								server.value.address(), targetVersion, version);
						return error("check.servers");
					}
				}
			} catch (ServerFault e) {
				logger.warn("Error while retrieving package versions of server {}", server.value.address(), e);
			}
		}

		return ok("check.servers");
	}

	public String getVersion(String name) {
		return name.substring(nodeJarName.length(), name.lastIndexOf(".jar"));
	}
}

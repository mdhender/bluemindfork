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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.dataprotect;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.helper.ArchiveHelper;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class CyrusSdsWorker extends DefaultWorker {
	private final Path outputPath = Paths.get("/var/backups/bluemind/sds");

	@Override
	public boolean supportsTag(String tag) {
		return "bm/core".equals(tag);
	}

	@Override
	public String getDataType() {
		return "sds";
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		SystemConf sysconf = LocalSysconfCache.get();
		if (!ArchiveHelper.isSdsArchiveKind(sysconf)) {
			return;
		}
		List<ItemValue<Domain>> domains = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).all().stream().filter(d -> !"global.virt".equals(d.uid))
				.collect(Collectors.toList());
		try {
			Path tempFolder = Files.createTempDirectory("sds-backup");
			try {
				logger.info("sds backup in {}", tempFolder);
				CyrusSdsBackup sdsbackup = new CyrusSdsBackup(tempFolder);
				sdsbackup.backupDomains(domains);
				// The node is required because we want files to be owned by root
				// even in JUnit tests
				INodeClient nc = NodeActivator.get(toBackup.value.address());
				NCUtils.execOrFail(nc, "mkdir -p " + outputPath);
				try (Stream<Path> stream = Files.list(tempFolder)) {
					stream.filter(p -> !Files.isDirectory(p)).forEach(p -> {
						try {
							nc.writeFile(outputPath.resolve(p.getFileName()).toString(), Files.newInputStream(p));
						} catch (ServerFault | IOException e) {
							logger.error("Unable to copy {} to {}: {}", tempFolder.resolve(p.getFileName()), outputPath,
									e.getMessage());
						}
					});
				}
			} finally {
				try (Stream<Path> stream = Files.walk(tempFolder)) {
					stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				}
			}
		} catch (IOException e) {
			logger.error("Unable to create temporary directory for sds backup");
		}
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(outputPath.toString());
	}
}

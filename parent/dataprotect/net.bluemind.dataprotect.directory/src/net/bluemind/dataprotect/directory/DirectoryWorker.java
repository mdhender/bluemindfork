/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.dataprotect.directory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.core.HollowConstants;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.directory.hollow.datamodel.producer.DirectorySerializer;
import net.bluemind.domain.api.IDomains;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class DirectoryWorker extends DefaultWorker {
	private static final String dir = "/var/backups/bluemind/work/directory";

	@Override
	public boolean supportsTag(String tag) {
		return "bm/core".equals(tag);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(dir);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		IServer srvApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		Optional<ItemValue<Server>> coreServer = srvApi.allComplete().stream()
				.filter(s -> s.value.tags.contains("bm/core")).findFirst();

		if (!coreServer.isPresent()) {
			throw new ServerFault("Unable to find server tagged as bm/core");
		}
		INodeClient nc = NodeActivator.get(coreServer.get().value.ip);

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).all().stream()
				.filter(d -> !"global.virt".equals(d.uid)).forEach(dom -> {
					DirectorySerializer ds = new DirectorySerializer(dom.uid);
					HollowConsumer.Blob blob = ds.getBlobRetriever()
							.retrieveSnapshotBlob(HollowConstants.VERSION_LATEST);

					String path = dir + "/" + dom.uid;
					NCUtils.waitFor(nc, nc.executeCommandNoOut(String.format("mkdir -p %s", path)));
					try {
						File snapshot = new File(path, String.format("snapshot-%s", blob.getToVersion()));
						nc.writeFile(snapshot.getPath(), blob.getInputStream());

						File version = new File(path, "announced.version");
						nc.writeFile(version.getPath(),
								new ByteArrayInputStream(Long.toString(blob.getToVersion()).getBytes()));
					} catch (IOException e) {
						throw new ServerFault("Unable to snapshot directory for domain " + dom.uid, e);
					}

				});
	}

	@Override
	public String getDataType() {
		return "directory";
	}
}

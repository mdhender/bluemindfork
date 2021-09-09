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
package net.bluemind.core.backup.continuous.events;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.hook.IMapiArtifactsHook;

public class MapiArtifactsHook implements IMapiArtifactsHook {

	private static final Logger logger = LoggerFactory.getLogger(MapiArtifactsHook.class);

	@Override
	public void onReplicaStored(String domainUid, MapiReplica mr) {
		ContainerDescriptor metaDesc = ContainerDescriptor.create(mr.mailboxUid + "_mapi_artifacts",
				mr.mailboxUid + " mapi_artifacts", mr.mailboxUid, "mapi_artifacts", domainUid, true);
		ItemValue<MapiReplica> replitem = ItemValue.create("replica", mr);
		replitem.internalId = replitem.uid.hashCode();
		replitem.created = new Date();
		IBackupStoreFactory store = DefaultBackupStore.store();
		store.<MapiReplica>forContainer(metaDesc).store(replitem).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			} else {
				logger.info("Pushed {} to {} via {}", mr, metaDesc, store);
			}
		});
	}

	@Override
	public void onMapiFolderStored(String domainUid, String ownerUid, MapiFolder mf) {

		ContainerDescriptor metaDesc = ContainerDescriptor.create(ownerUid + "_mapi_artifacts",
				ownerUid + " mapi_artifacts", ownerUid, "mapi_artifacts", domainUid, true);
		ItemValue<MapiFolder> folder = ItemValue.create(mf.containerUid, mf);
		folder.internalId = folder.uid.hashCode();
		folder.created = new Date();
		IBackupStoreFactory store = DefaultBackupStore.store();
		store.<MapiFolder>forContainer(metaDesc).store(folder).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			} else {
				logger.info("Pushed {} to {} via {}", mf, metaDesc, store);
			}
		});
	}

}

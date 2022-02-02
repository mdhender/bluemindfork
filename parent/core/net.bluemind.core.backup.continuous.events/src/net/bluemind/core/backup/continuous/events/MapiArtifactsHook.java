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

import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.hook.IMapiArtifactsHook;

public class MapiArtifactsHook implements IMapiArtifactsHook {
	private static final String TYPE = "mapi_artifacts";
	private MapiReplicaContinuousBackup replicaBackup = new MapiReplicaContinuousBackup();
	private MapiFolderContinuousBackup folderBackup = new MapiFolderContinuousBackup();

	public class MapiReplicaContinuousBackup implements ContinuousContenairization<MapiReplica> {
		@Override
		public String type() {
			return TYPE;
		}
	}

	public class MapiFolderContinuousBackup implements ContinuousContenairization<MapiFolder> {
		@Override
		public String type() {
			return TYPE;
		}
	}

	@Override
	public void onReplicaStored(String domainUid, MapiReplica mr) {
		replicaBackup.save(domainUid, mr.mailboxUid, "replica", mr, true);
	}

	@Override
	public void onMapiFolderStored(String domainUid, String ownerUid, MapiFolder mf) {
		folderBackup.save(domainUid, ownerUid, mf.containerUid, mf, true);
	}

}

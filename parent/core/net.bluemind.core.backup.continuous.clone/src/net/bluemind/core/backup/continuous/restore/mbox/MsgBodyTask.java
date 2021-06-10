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
package net.bluemind.core.backup.continuous.restore.mbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.bluemind.core.backup.continuous.restore.CloneException;
import net.bluemind.core.backup.continuous.restore.mbox.UidDatalocMapping.Replica;
import net.bluemind.core.backup.continuous.syncclient.SyncClientOIO;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsSyncStore;

public class MsgBodyTask {

	private ISdsSyncStore sds;
	private Replica replica;
	private SyncClientOIO syncClient;

	public MsgBodyTask(ISdsSyncStore sdsStore, SyncClientOIO sc, Replica repl) {
		this.sds = sdsStore;
		this.syncClient = sc;
		this.replica = repl;
	}

	public int run(IServerTaskMonitor mon, String guid) {
		Path dl = null;
		try {
			GetRequest get = new GetRequest();
			get.guid = guid;
			dl = Files.createTempFile("eml", ".sds");
			SdsResponse dlRes = sds.download(GetRequest.of("x", guid, dl.toFile().getAbsolutePath()));
			if (dlRes.succeeded()) {
				byte[] emlData = Files.readAllBytes(dl);
				String res = syncClient.applyMessage(replica.part.name, guid, emlData);
				mon.log("Processed body " + guid + " APPLY MSG: " + res);
				return emlData.length;
			} else {
				throw new CloneException("Missing body " + guid + ": " + dlRes.error);
			}
		} catch (Exception e) {
			throw CloneException.propagate(e);
		} finally {
			if (dl != null) {
				try {
					Files.delete(dl);
				} catch (IOException e) {
					// yeah
				}
			}
		}
	}

}

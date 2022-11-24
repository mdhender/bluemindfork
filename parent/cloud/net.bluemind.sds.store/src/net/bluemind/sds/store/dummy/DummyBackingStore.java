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
package net.bluemind.sds.store.dummy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.dto.TierMoveRequest;
import net.bluemind.sds.dto.TierMoveResponse;
import net.bluemind.sds.store.ISdsBackingStore;

public class DummyBackingStore implements ISdsBackingStore {
	private static final Logger logger = LoggerFactory.getLogger(DummyBackingStore.class);

	private static final File root = new File(System.getProperty("user.home"), "dummy-sds");
	static {
		root.mkdirs();
	}

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest exist) {
		ExistResponse resp = new ExistResponse();
		resp.exists = new File(root, exist.guid).exists();
		return CompletableFuture.completedFuture(resp);
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest put) {
		File dst = new File(root, put.guid);
		if (!dst.exists()) {
			File source = new File(put.filename);
			try {
				Files.copy(source.toPath(), dst.toPath());
			} catch (IOException e) {
				CompletableFuture<SdsResponse> f = new CompletableFuture<>();
				f.completeExceptionally(e);
				return f;
			}
		}
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest get) {
		File source = new File(root, get.guid);
		File dest = new File(get.filename);
		if (dest.exists()) {
			logger.warn("{} already exist", dest.getAbsolutePath());
		} else {
			try {
				Files.copy(source.toPath(), dest.toPath());
			} catch (IOException e) {
				CompletableFuture<SdsResponse> f = new CompletableFuture<>();
				f.completeExceptionally(e);
				return f;
			}
		}
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest del) {
		new File(root, del.guid).delete();
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<TierMoveResponse> tierMove(TierMoveRequest tierMoveRequest) {
		return CompletableFuture.completedFuture(new TierMoveResponse(
				tierMoveRequest.moves.stream().map(tm -> tm.messageBodyGuid).toList(), Collections.emptyList()));
	}

	@Override
	public void close() {
		// Nothing to do
	}

}

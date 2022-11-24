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
package net.bluemind.sds.store.noop;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.dto.TierMoveRequest;
import net.bluemind.sds.dto.TierMoveResponse;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;

public class NoopSyncStore implements ISdsBackingStore {
	public static final ISdsBackingStoreFactory FACTORY = new ISdsBackingStoreFactory() {

		@Override
		public ArchiveKind kind() {
			return ArchiveKind.Noop;
		}

		@Override
		public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
			return new NoopSyncStore();
		}
	};

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest exist) {
		return CompletableFuture.completedFuture(ExistResponse.from(false));
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest put) {
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest get) {
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest del) {
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

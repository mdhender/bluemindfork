/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.tests;

import java.util.concurrent.CompletableFuture;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.ISdsBackingStoreFactory;
import net.bluemind.system.api.ArchiveKind;

public class SdsTestStore implements ISdsBackingStore {

	private static final SdsTestStore INST = new SdsTestStore();

	public static class StoreFactory implements ISdsBackingStoreFactory {

		@Override
		public ArchiveKind kind() {
			return ArchiveKind.Test;
		}

		@Override
		public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
			INST.setVertx(vertx);
			vertx.eventBus().publish("test.store.configured", configuration);
			return INST;
		}

	}

	private Vertx vertx;

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest req) {
		vertx.eventBus().publish("test.store.exists", req.guid);
		ExistResponse er = new ExistResponse();
		er.exists = true;
		return CompletableFuture.completedFuture(er);
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest req) {
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		return CompletableFuture.completedFuture(new SdsResponse());
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest req) {
		return CompletableFuture.completedFuture(new SdsResponse());
	}

}

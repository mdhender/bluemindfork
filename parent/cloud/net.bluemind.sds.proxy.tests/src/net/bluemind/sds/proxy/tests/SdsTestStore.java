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

import java.io.IOException;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.sds.proxy.dto.DeleteRequest;
import net.bluemind.sds.proxy.dto.ExistRequest;
import net.bluemind.sds.proxy.dto.ExistResponse;
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.dto.PutRequest;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.ISdsBackingStoreFactory;

public class SdsTestStore implements ISdsBackingStore {

	private static final SdsTestStore INST = new SdsTestStore();

	public static class StoreFactory implements ISdsBackingStoreFactory {

		@Override
		public String name() {
			return "test";
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
	public ExistResponse exists(ExistRequest req) {
		vertx.eventBus().publish("test.store.exists", req.guid);
		ExistResponse er = new ExistResponse();
		er.exist = true;
		return er;
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public SdsResponse upload(PutRequest req) throws IOException {
		return null;
	}

	@Override
	public SdsResponse download(GetRequest req) throws IOException {
		return null;
	}

	@Override
	public SdsResponse delete(DeleteRequest req) {
		return null;
	}

}

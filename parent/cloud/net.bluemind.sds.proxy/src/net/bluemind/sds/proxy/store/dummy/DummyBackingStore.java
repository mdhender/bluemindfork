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
package net.bluemind.sds.proxy.store.dummy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class DummyBackingStore implements ISdsBackingStore {

	private static final Logger logger = LoggerFactory.getLogger(DummyBackingStore.class);

	public static final ISdsBackingStoreFactory FACTORY = new ISdsBackingStoreFactory() {

		@Override
		public String name() {
			return "dummy";
		}

		@Override
		public ISdsBackingStore create(Vertx vertx, JsonObject configuration) {
			return new DummyBackingStore();
		}
	};

	@Override
	public ExistResponse exists(ExistRequest exist) {
		ExistResponse resp = new ExistResponse();
		resp.exist = new File("/dummy-sds/", exist.guid).exists();
		return resp;
	}

	@Override
	public SdsResponse upload(PutRequest put) throws IOException {
		File dst = new File("/dummy-sds", put.guid);
		if (!dst.exists()) {
			File source = new File(put.filename);
			Files.copy(source.toPath(), dst.toPath());
		}
		return new SdsResponse();
	}

	@Override
	public SdsResponse download(GetRequest get) throws IOException {
		File source = new File("/dummy-sds", get.guid);
		File dest = new File(get.filename);
		if (dest.exists()) {
			logger.warn("{} already exist", dest.getAbsolutePath());
		} else {
			Files.copy(source.toPath(), dest.toPath());
		}
		return new SdsResponse();
	}

	@Override
	public SdsResponse delete(DeleteRequest del) {
		boolean deleted = new File("/dummy-sds/", del.guid).delete();
		logger.info("Deleted ? {}", deleted);
		return new SdsResponse();
	}

}

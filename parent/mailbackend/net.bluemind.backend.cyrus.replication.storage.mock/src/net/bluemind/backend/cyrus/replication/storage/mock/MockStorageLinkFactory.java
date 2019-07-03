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
package net.bluemind.backend.cyrus.replication.storage.mock;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.backend.cyrus.replication.server.state.StorageLinkFactory;
import net.bluemind.core.rest.http.HttpClientProvider;

public class MockStorageLinkFactory implements StorageLinkFactory {

	private static final Logger logger = LoggerFactory.getLogger(MockStorageLinkFactory.class);

	public MockStorageLinkFactory() {
		logger.info("Mock storage link factory created.");
	}

	@Override
	public CompletableFuture<StorageApiLink> newLink(Vertx vertx, HttpClientProvider http, String remoteIp) {
		return CompletableFuture.completedFuture(new MockReplicationStorage(vertx, http, remoteIp));
	}

	@Override
	public boolean isAvailable() {
		return true;
	}

}

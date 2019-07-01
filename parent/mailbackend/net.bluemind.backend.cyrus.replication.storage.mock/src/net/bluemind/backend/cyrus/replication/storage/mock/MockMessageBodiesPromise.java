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
package net.bluemind.backend.cyrus.replication.storage.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodiesPromise;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

public class MockMessageBodiesPromise implements IDbMessageBodiesPromise {

	private static final Logger logger = LoggerFactory.getLogger(MockMessageBodiesPromise.class);
	private final String part;

	public MockMessageBodiesPromise(String partition) {
		this.part = partition;
		logger.debug("part is {}", this.part);
	}

	@Override
	public CompletableFuture<List<MessageBody>> multiple(List<String> uid) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<Void> update(MessageBody body) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> delete(String uid) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<MessageBody> getComplete(String uid) {
		CompletableFuture<MessageBody> ret = new CompletableFuture<>();
		ret.completeExceptionally(ServerFault.notFound(uid + " not found."));
		return ret;
	}

	@Override
	public CompletableFuture<List<String>> missing(List<String> toCheck) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<Void> create(String uid, Stream eml) {
		logger.info("Mock create of {}", uid);
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Boolean> exists(String uid) {
		logger.info("Mock {} does not exist", uid);
		return CompletableFuture.completedFuture(false);
	}

}

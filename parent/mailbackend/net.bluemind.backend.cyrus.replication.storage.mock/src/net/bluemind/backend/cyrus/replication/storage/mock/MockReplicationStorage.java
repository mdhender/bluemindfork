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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotationsPromise;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifactsPromise;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbMessageBodiesPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.ResolvedMailbox;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.vertx.VertxStream;

public class MockReplicationStorage implements StorageApiLink {

	private static final Logger logger = LoggerFactory.getLogger(MockReplicationStorage.class);
	private final Vertx vertx;
	private final HttpClientProvider clientProv;
	private String remoteIp;

	public MockReplicationStorage(Vertx vertx, HttpClientProvider http, String remoteIp) {
		this.vertx = vertx;
		this.clientProv = http;
		this.remoteIp = remoteIp;
		logger.info("Created with {} and {}", this.vertx, this.clientProv);
	}

	@Override
	public String remoteIp() {
		return remoteIp;
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public CompletableFuture<ApiDesc> replicatedMailboxes(ReplicatedBox box) {
		return null;
	}

	@Override
	public CompletableFuture<Boolean> validate(String login, String secret) {
		return null;
	}

	@Override
	public CompletableFuture<IDbMessageBodiesPromise> bodies(String partition) {
		return CompletableFuture.completedFuture(new MockMessageBodiesPromise(partition));
	}

	@Override
	public CompletableFuture<IDbReplicatedMailboxesPromise> replicatedMailboxes(String partition,
			MailboxReplicaRootDescriptor root) {
		return BrokenPromises.withServerFault("mock replicated mailboxes " + partition + " " + root);
	}

	@Override
	public CompletableFuture<IDbMailboxRecordsPromise> mailboxRecords(String mboxUniqueId) {
		return BrokenPromises.withServerFault("mock records " + mboxUniqueId);
	}

	@Override
	public CompletableFuture<ICyrusReplicationArtifactsPromise> cyrusArtifacts(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<ICyrusReplicationAnnotationsPromise> cyrusAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CompletableFuture<Void> delete(MailboxReplicaRootDescriptor root, String partition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream stream(Path p) {
		return VertxStream.localPath(p);
	}

	@Override
	public CompletableFuture<List<ResolvedMailbox>> resolveNames(List<String> names) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotationsPromise;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifactsPromise;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbMessageBodiesPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public interface StorageApiLink {

	public static final Logger logger = LoggerFactory.getLogger(StorageApiLink.class);

	public static class ApiDesc {

		IDbReplicatedMailboxesPromise mboxApi;
		MailboxReplicaRootDescriptor rootDesc;
		String partition;

		public ApiDesc(String partition, MailboxReplicaRootDescriptor root, IDbReplicatedMailboxesPromise mboxApi) {
			this.partition = partition;
			this.rootDesc = root;
			this.mboxApi = mboxApi;
		}

	}

	public String remoteIp();

	public void release();

	public static CompletableFuture<StorageApiLink> create(Vertx vertx, HttpClientProvider http, String remoteIp) {
		RunnableExtensionLoader<StorageLinkFactory> rel = new RunnableExtensionLoader<>();
		List<StorageLinkFactory> factories = rel.loadExtensionsWithPriority(
				"net.bluemind.backend.cyrus.replication.server", "storage", "storage", "factory");
		Optional<StorageLinkFactory> factory = factories.stream().filter(link -> link.isAvailable()).findFirst();
		return factory.map(f -> {
			logger.info("Selected {}", f);
			return f.newLink(vertx, http, remoteIp);
		}).orElseThrow(() -> ReplicationException.serverError("No StorageApiLink usable implementation found."));
	}

	public Stream stream(Path p);

	public CompletableFuture<IDbMessageBodiesPromise> bodies(String partition);

	public CompletableFuture<IDbReplicatedMailboxesPromise> replicatedMailboxes(String partition,
			MailboxReplicaRootDescriptor root);

	public CompletableFuture<IDbMailboxRecordsPromise> mailboxRecords(String mboxUniqueId);

	/**
	 * Returns the API object suitable to manipulate the given mailbox.
	 * 
	 * @param box can't be null
	 * @return access to db hierarchy api
	 */
	public CompletableFuture<ApiDesc> replicatedMailboxes(ReplicatedBox box);

	public CompletableFuture<ICyrusReplicationArtifactsPromise> cyrusArtifacts(String userId);

	public CompletableFuture<ICyrusReplicationAnnotationsPromise> cyrusAnnotations();

	public CompletableFuture<Boolean> validate(String login, String secret);

	public CompletableFuture<Void> delete(MailboxReplicaRootDescriptor root, String partition);

}

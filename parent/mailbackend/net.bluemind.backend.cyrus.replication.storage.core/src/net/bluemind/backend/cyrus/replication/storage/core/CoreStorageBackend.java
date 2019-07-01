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
package net.bluemind.backend.cyrus.replication.storage.core;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;

import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.backend.cyrus.replication.server.state.StorageLinkFactory;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotationsPromise;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifactsPromise;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbMessageBodiesPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmtPromise;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.config.Token;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.PromiseServiceProvider;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.locator.vertxclient.VertxLocatorClient;

public class CoreStorageBackend implements StorageApiLink {
	private static final Logger logger = LoggerFactory.getLogger(CoreStorageBackend.class);

	private static final boolean SYNC_DEBUG = new File(System.getProperty("user.home") + "/sync.debug").exists();

	private Set<String> validatedPartitions;
	private Set<String> validatedRoots;
	private IServiceProvider asyncProv;

	private String remoteIp;

	private CoreStorageBackend(IServiceProvider asyncProv, String remoteIp) {
		this.asyncProv = asyncProv;
		this.remoteIp = remoteIp;
		validatedPartitions = new HashSet<>();
		validatedRoots = new HashSet<>();
	}

	public String remoteIp() {
		return remoteIp;
	}

	public void release() {
		asyncProv.instance(IAuthenticationPromise.class).logout().thenAccept(v -> logger.info("Session invalidated."));
	}

	public static class HttpLinkFactory implements StorageLinkFactory {

		@Override
		public CompletableFuture<StorageApiLink> newLink(Vertx vertx, HttpClientProvider http, String remoteIp) {
			VertxLocatorClient vlc = new VertxLocatorClient(http, "admin0@global.virt");
			VertxPromiseServiceProvider prom = new VertxPromiseServiceProvider(http, vlc, Token.admin0());
			logger.info("HTTP MODE, using http client");

			IServiceProvider prov = prom;
			IAuthenticationPromise asyncAuth = prov.instance(IAuthenticationPromise.class);
			return asyncAuth.ping().thenApply(v -> {
				StorageApiLink apiLink = new CoreStorageBackend(prov, remoteIp);
				logger.info("[{}] Api link created, core pinged.", remoteIp);
				return apiLink;
			});
		}

		@Override
		public boolean isAvailable() {
			return SYNC_DEBUG;
		}

	}

	public static class InCoreDirectLinkFactory implements StorageLinkFactory {

		@Override
		public CompletableFuture<StorageApiLink> newLink(Vertx vertx, HttpClientProvider http, String remoteIp) {
			logger.info("IN-CORE MODE, using promise client");
			SecurityContext sysCtx = SecurityContext.SYSTEM;
			SecurityContext systemWithSID = new SecurityContext(Token.admin0(), sysCtx.getSubject(),
					sysCtx.getMemberOf(), sysCtx.getRoles(), "global.virt");
			// the proxy generated here needs a sessionId in the security
			// context to work correctly
			IServiceProvider prov = PromiseServiceProvider.getProvider(vertx, systemWithSID);

			IAuthenticationPromise asyncAuth = prov.instance(IAuthenticationPromise.class);
			return asyncAuth.ping().thenApply(v -> {
				StorageApiLink apiLink = new CoreStorageBackend(prov, remoteIp);
				logger.info("[{}] Api link created, core pinged.", remoteIp);
				return apiLink;
			});
		}

		@Override
		public boolean isAvailable() {
			return !SYNC_DEBUG;
		}

	}

	public CompletableFuture<IDbMessageBodiesPromise> bodies(String partition) {
		CompletableFuture<IDbMessageBodiesPromise> apiProm = new CompletableFuture<>();
		if (!validatedPartitions.contains(partition)) {
			logger.info("Partition {} bodies setup complete.", partition);
			if (!apiProm.isCompletedExceptionally()) {
				validatedPartitions.add(partition);
				apiProm.complete(asyncProv.instance(IDbMessageBodiesPromise.class, partition));
			}
		} else {
			apiProm.complete(asyncProv.instance(IDbMessageBodiesPromise.class, partition));
		}

		return apiProm;
	}

	public CompletableFuture<IDbReplicatedMailboxesPromise> replicatedMailboxes(String partition,
			MailboxReplicaRootDescriptor root) {
		String rootString = partition + "!" + root.fullName();
		logger.debug("Checking {}...", rootString);
		CompletableFuture<IDbReplicatedMailboxesPromise> apiProm = new CompletableFuture<>();
		if (!validatedRoots.contains(rootString)) {
			IReplicatedMailboxesRootMgmtPromise mgmtApi = asyncProv.instance(IReplicatedMailboxesRootMgmtPromise.class,
					partition);
			mgmtApi.create(root).thenAccept(v -> {
				logger.info("Root {} setup complete.", rootString);
				validatedRoots.add(rootString);
				apiProm.complete(mboxesApi(partition, root.fullName()));
			}).exceptionally(t -> {
				logger.error(t.getMessage(), t);
				apiProm.completeExceptionally(t);
				return null;
			});
		} else {
			try {
				apiProm.complete(mboxesApi(partition, root.fullName()));
			} catch (Exception e) {
				apiProm.completeExceptionally(e);
			}
		}
		return apiProm;
	}

	private IDbReplicatedMailboxesPromise mboxesApi(String partition, String root) {
		logger.debug("RepMbox API for {}/{}", partition, root);
		return asyncProv.instance(IDbReplicatedMailboxesPromise.class, partition, root);
	}

	public CompletableFuture<IDbMailboxRecordsPromise> mailboxRecords(String mboxUniqueId) {
		// this method returns a promise for consistency with the other ones
		// doing async stuff
		CompletableFuture<IDbMailboxRecordsPromise> apiProm = new CompletableFuture<>();
		apiProm.complete(asyncProv.instance(IDbMailboxRecordsPromise.class, mboxUniqueId));
		return apiProm;
	}

	/**
	 * Returns the API object suitable to manipulate the given mailbox.
	 * 
	 * @param box can't be null
	 * @return access to db hierarchy api
	 */
	public CompletableFuture<ApiDesc> replicatedMailboxes(ReplicatedBox box) {
		CompletableFuture<ApiDesc> ret = new CompletableFuture<>();
		MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor.create(box.ns, box.local);
		replicatedMailboxes(box.partition, root).thenAccept(mboxApi -> {
			ret.complete(new ApiDesc(box.partition, root, mboxApi));
		});
		return ret;
	}

	public CompletableFuture<ICyrusReplicationArtifactsPromise> cyrusArtifacts(String userId) {
		ICyrusReplicationArtifactsPromise promApi = asyncProv.instance(ICyrusReplicationArtifactsPromise.class, userId);
		return CompletableFuture.completedFuture(promApi);
	}

	public CompletableFuture<ICyrusReplicationAnnotationsPromise> cyrusAnnotations() {
		ICyrusReplicationAnnotationsPromise promApi = asyncProv.instance(ICyrusReplicationAnnotationsPromise.class);
		return CompletableFuture.completedFuture(promApi);
	}

	public CompletableFuture<Boolean> validate(String login, String secret) {
		IAuthenticationPromise authApi = asyncProv.instance(IAuthenticationPromise.class);
		return authApi.validate(login, secret, "replication-auth").thenApply(validation -> {
			switch (validation) {
			case TOKEN:
			case PASSWORD:
				return true;
			default:
				return false;
			}
		});
	}

	public CompletableFuture<Void> delete(MailboxReplicaRootDescriptor root, String partition) {
		return asyncProv.instance(IReplicatedMailboxesRootMgmtPromise.class, partition).delete(root);
	}
}

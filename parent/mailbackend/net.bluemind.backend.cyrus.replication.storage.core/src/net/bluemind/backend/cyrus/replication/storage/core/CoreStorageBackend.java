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
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.backend.cyrus.replication.server.state.StorageLinkFactory;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotationsPromise;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifactsPromise;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbMessageBodiesPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmtPromise;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmtPromise;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.ResolvedMailbox;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Stream;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.PromiseServiceProvider;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;

public class CoreStorageBackend implements StorageApiLink {
	private static final Logger logger = LoggerFactory.getLogger(CoreStorageBackend.class);

	private static final boolean SYNC_DEBUG = new File(System.getProperty("user.home") + "/sync.debug").exists();

	private final IServiceProvider asyncProv;
	private final String remoteIp;
	private final StreamProvider streamProvider;

	private interface StreamProvider {
		Stream of(Path p);
	}

	private CoreStorageBackend(IServiceProvider asyncProv, String remoteIp, StreamProvider sp) {
		this.asyncProv = asyncProv;
		this.remoteIp = remoteIp;
		this.streamProvider = sp;
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
			ILocator vlc = (String service, AsyncHandler<String[]> asyncHandler) -> {
				asyncHandler.success(new String[] {
						Topology.getIfAvailable().map(t -> t.core().value.address()).orElse("127.0.0.1") });
			};
			VertxPromiseServiceProvider prom = new VertxPromiseServiceProvider(http, vlc, Token.admin0());
			logger.info("HTTP MODE, using http client");

			IServiceProvider prov = prom;
			IAuthenticationPromise asyncAuth = prov.instance(IAuthenticationPromise.class);
			return asyncAuth.ping().thenApply(v -> {
				StorageApiLink apiLink = new CoreStorageBackend(prov, remoteIp, path -> {
					try (FileChannel channel = (FileChannel) Files.newByteChannel(path, StandardOpenOption.READ)) {
						MappedByteBuffer mapped = channel.map(MapMode.READ_ONLY, 0, channel.size());
						return VertxStream.stream(Buffer.buffer(Unpooled.wrappedBuffer(mapped)));
					} catch (IOException ie) {
						throw new RuntimeException(ie);
					}
				});
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
				StorageApiLink apiLink = new CoreStorageBackend(prov, remoteIp, p -> VertxStream.localPath(p));
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
		if (!KnownRoots.validatedPartitions.contains(partition)) {
			return CompletableFuture.completedFuture(asyncProv.instance(IDbMessageBodiesPromise.class, partition))
					.thenApply(v -> {
						logger.info("Partition {} bodies setup complete.", partition);
						KnownRoots.validatedPartitions.add(partition);
						return v;
					});
		} else {
			return CompletableFuture.completedFuture(asyncProv.instance(IDbMessageBodiesPromise.class, partition));
		}
	}

	public CompletableFuture<IDbReplicatedMailboxesPromise> replicatedMailboxes(String partition,
			MailboxReplicaRootDescriptor root) {
		String rootString = partition + "!" + root.fullName();
		logger.debug("Checking {}...", rootString);
		if (!KnownRoots.validatedRoots.contains(rootString)) {
			IReplicatedMailboxesRootMgmtPromise mgmtApi = asyncProv.instance(IReplicatedMailboxesRootMgmtPromise.class,
					partition);
			return mgmtApi.create(root).thenApply(v -> {
				logger.info("Root {} setup complete.", rootString);
				KnownRoots.validatedRoots.add(rootString);
				return mboxesApi(partition, root.fullName());
			});
		} else {
			return CompletableFuture.completedFuture(mboxesApi(partition, root.fullName()));
		}
	}

	private IDbReplicatedMailboxesPromise mboxesApi(String partition, String root) {
		logger.debug("RepMbox API for {}/{}", partition, root);
		return asyncProv.instance(IDbReplicatedMailboxesPromise.class, partition, root);
	}

	public CompletableFuture<IDbMailboxRecordsPromise> mailboxRecords(String mboxUniqueId) {
		// this method returns a promise for consistency with the other ones
		// doing async stuff
		return CompletableFuture.completedFuture(asyncProv.instance(IDbMailboxRecordsPromise.class, mboxUniqueId));
	}

	/**
	 * Returns the API object suitable to manipulate the given mailbox.
	 * 
	 * @param box can't be null
	 * @return access to db hierarchy api
	 */
	public CompletableFuture<ApiDesc> replicatedMailboxes(ReplicatedBox box) {
		MailboxReplicaRootDescriptor root = MailboxReplicaRootDescriptor.create(box.ns, box.local);
		return replicatedMailboxes(box.partition, root).thenApply(mboxApi -> new ApiDesc(box.partition, root, mboxApi));
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

	@Override
	public Stream stream(Path p) {
		return streamProvider.of(p);
	}

	@Override
	public CompletableFuture<List<ResolvedMailbox>> resolveNames(List<String> names) {
		return asyncProv.instance(IReplicatedMailboxesMgmtPromise.class).resolve(names);
	}
}

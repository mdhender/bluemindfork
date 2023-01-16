/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.sds.store.cyrusspool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.luben.zstd.RecyclingBufferPool;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.partitions.MailboxDescriptor;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.backend.mail.replica.api.Tier;
import net.bluemind.backend.mail.replica.api.TierMove;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsError;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.dto.TierMoveRequest;
import net.bluemind.sds.dto.TierMoveResponse;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class SpoolBackingStore implements ISdsBackingStore {

	private static final Logger logger = LoggerFactory.getLogger(SpoolBackingStore.class);
	private final IServiceProvider prov;
	private final SystemConf sysconf;
	private ItemValue<Server> backend;
	private final INodeClient nc;

	public SpoolBackingStore(@SuppressWarnings("unused") Vertx vertx, IServiceProvider prov,
			ItemValue<Server> backend) {
		this.prov = prov;
		this.backend = backend;
		this.sysconf = LocalSysconfCache.get();
		this.nc = NodeActivator.get(backend.value.address());
	}

	@Override
	public CompletableFuture<SdsResponse> upload(PutRequest req) {
		String target = chooseTarget(req);

		ByteBuf bb = Unpooled.buffer(2 * (int) new File(req.filename).length());
		try (InputStream input = Files.newInputStream(Paths.get(req.filename));
				ByteBufOutputStream bbo = new ByteBufOutputStream(bb);
				OutputStream zst = new ZstdOutputStream(bbo, RecyclingBufferPool.INSTANCE, -3)) {
			long copied = ByteStreams.copy(input, zst);
			logger.info("Compressed {}byte(s) for {}", copied, req.guid);
		} catch (IOException e) {
			return exception(e);
		}
		try (InputStream input = new ByteBufInputStream(bb, true)) {
			nc.writeFile(target, input);
			return CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
		} catch (IOException e) {
			return exception(e);
		}

	}

	/*
	 * Returns the path to store the new message, watching deliveryDate to choose
	 * the correct storage tier
	 */
	private String chooseTarget(PutRequest req) {
		if (req.deliveryDate == null) {
			return livePath(req.guid);
		}
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		Integer archiveDays = sysconf.integerValue(SysConfKeys.archive_days.name(), 0);
		if (archiveKind != null && archiveKind.supportsHsm() && archiveDays > 0
				&& req.deliveryDate.toInstant().isBefore(Instant.now().plus(archiveDays, ChronoUnit.DAYS))) {
			// deliveryDate is before now() + tierChangeDelay => direct insert to archive
			return archivePath(req.guid);
		} else {
			return livePath(req.guid);
		}
	}

	private static CompletableFuture<SdsResponse> exception(Throwable t) {
		CompletableFuture<SdsResponse> sr = new CompletableFuture<>();
		sr.completeExceptionally(t);
		return sr;
	}

	@Override
	public CompletableFuture<ExistResponse> exists(ExistRequest req) {
		return locateGuid(null, req.guid, false).thenApply(sdsResp -> ExistResponse.from(sdsResp.succeeded()));
	}

	@Override
	public CompletableFuture<SdsResponse> download(GetRequest req) {
		return locateGuid(req.filename, req.guid, true);
	}

	@Override
	public CompletableFuture<SdsResponse> downloadRaw(GetRequest req) {
		return locateGuid(req.filename, req.guid, false);
	}

	private CompletableFuture<SdsResponse> locateGuid(String targetPath, String guid, boolean decompress) {
		// check new live path
		String path = livePath(guid);
		if (locatePath(targetPath, path, decompress)) {
			return CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
		}

		// check new archive path
		path = archivePath(guid);
		if (locatePath(targetPath, path, decompress)) {
			return CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
		}

		// check old cyrus stuff
		IReplicatedMailboxesMgmt mgmtApi = prov.instance(IReplicatedMailboxesMgmt.class);
		Set<MailboxRecordItemUri> refs = mgmtApi.getBodyGuidReferences(guid);
		if (!refs.isEmpty()) {
			MailboxRecordItemUri uri = refs.iterator().next();
			IContainers contApi = prov.instance(IContainers.class);
			ContainerDescriptor cont = contApi.getIfPresent(uri.containerUid);
			IMailboxes mboxes = prov.instance(IMailboxes.class, cont.domainUid);
			ItemValue<Mailbox> mbox = mboxes.getComplete(uri.owner);
			IDbReplicatedMailboxes folderApi = prov.instance(IDbReplicatedMailboxes.class, cont.domainUid,
					mbox.value.type.nsPrefix + mbox.value.name);
			ItemValue<MailboxFolder> folder = folderApi.getComplete(uri.containerUid);
			if (folder != null) { // Broken user folder ?
				ItemValue<Server> server = Topology.get().datalocation(cont.datalocation);
				CyrusPartition part = CyrusPartition.forServerAndDomain(server, cont.domainUid);
				MailboxDescriptor desc = new MailboxDescriptor();
				desc.type = mbox.value.type;
				desc.mailboxName = mbox.value.name;
				desc.utf7FolderPath = UTF7Converter.encode(folder.value.fullName);
				// compute the fucking path...
				path = CyrusFileSystemPathHelper.getFileSystemPath(cont.domainUid, desc, part, uri.imapUid);
				if (onNode(targetPath, path, server, decompress)) {
					logger.info("{} -> '{}'", guid, path);
					CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
				}
				path = CyrusFileSystemPathHelper.getHSMFileSystemPath(cont.domainUid, desc, part, uri.imapUid);
				if (onNode(targetPath, path, server, decompress)) {
					logger.info("{} -> '{}'", guid, path);
					CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
				}
			}
		}
		SdsResponse sr = new SdsResponse();
		sr.error = new SdsError(guid + " not found.");
		return CompletableFuture.completedFuture(sr);
	}

	@Override
	public CompletableFuture<TierMoveResponse> tierMove(TierMoveRequest tierMoveRequest) {
		logger.debug("Tier move request {}", tierMoveRequest);
		List<String> errors = new ArrayList<>();
		List<String> successes = new ArrayList<>();
		for (TierMove move : tierMoveRequest.moves) {
			String from;
			String to;

			if (move.tier.equals(Tier.SLOW)) {
				from = livePath(move.messageBodyGuid);
				to = archivePath(move.messageBodyGuid);
			} else {
				from = archivePath(move.messageBodyGuid);
				to = livePath(move.messageBodyGuid);
			}

			if (nc.exists(from)) {
				try {
					nc.moveFile(from, to);
					successes.add(move.messageBodyGuid);
				} catch (ServerFault e) {
					errors.add(move.messageBodyGuid);
				}
			}
		}
		return CompletableFuture.completedFuture(new TierMoveResponse(successes, errors));
	}

	private boolean locatePath(String targetPath, String emlPath, boolean decompress) {
		return onNode(targetPath, emlPath, backend, decompress);
	}

	private boolean onNode(String targetPath, String emlPath, ItemValue<Server> b, boolean decompress) {
		if (targetPath == null) {
			return nc.exists(emlPath);
		} else {
			byte[] eml = nc.read(emlPath);
			if (eml.length > 0) {
				logger.info("Found {} byte(s) of mail data in {}, tgt is {}{}", eml.length, emlPath, targetPath,
						(decompress ? "" : " (compressed)"));
				if (decompress && emlPath.endsWith(".zst")) {
					return compressedEml(targetPath, eml);
				} else {
					return plainEml(targetPath, eml);
				}
			}
		}
		return false;
	}

	private boolean plainEml(String targetPath, byte[] eml) {
		try {
			Files.write(Paths.get(targetPath), eml);
			logger.info("Wrote plain {} byte(s) to {}", eml.length, targetPath);
			return true;
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	private boolean compressedEml(String targetPath, byte[] eml) {
		ByteBufInputStream oio = new ByteBufInputStream(Unpooled.wrappedBuffer(eml));
		try (ZstdInputStream in = new ZstdInputStream(oio, RecyclingBufferPool.INSTANCE);
				OutputStream out = Files.newOutputStream(Paths.get(targetPath))) {
			long copied = in.transferTo(out);
			out.flush();
			logger.info("Wrote compressed {} byte(s) to {}", copied, targetPath);
			return true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	private String livePath(String guid) {
		return "/var/spool/cyrus/data/by_hash/" + guid.charAt(0) + "/" + guid.charAt(1) + "/" + guid + ".zst";
	}

	private String archivePath(String guid) {
		return "/var/spool/bm-hsm/data/by_hash/" + guid.charAt(0) + "/" + guid.charAt(1) + "/" + guid + ".zst";
	}

	@Override
	public CompletableFuture<SdsResponse> delete(DeleteRequest req) {
		for (Path p : Arrays.asList(Paths.get(livePath(req.guid)), Paths.get(archivePath(req.guid)))) {
			try {
				if (Files.deleteIfExists(p)) {
					logger.info("removed {}: {}", req.guid, p);
					return CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
				}
			} catch (IOException e) {
				// Nothing to worry about
			}
		}
		return CompletableFuture.completedFuture(SdsResponse.UNTAGGED_OK);
	}

	@Override
	public void close() {
		// that's ok
	}

}

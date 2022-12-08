/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.netty.buffer.ByteBufUtil;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IMessageBodyTierChange;
import net.bluemind.backend.mail.replica.api.Tier;
import net.bluemind.backend.mail.replica.api.TierMove;
import net.bluemind.backend.mail.replica.persistence.MessageBodyTierChangeQueueStore;
import net.bluemind.backend.mail.replica.persistence.MessageBodyTierChangeQueueStore.TierAddResult;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ArchiveKind;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class MessageBodyTierChangeService implements IMessageBodyTierChange {
	private static final Logger logger = LoggerFactory.getLogger(MessageBodyTierChangeService.class);

	private final BmContext context;
	private final MessageBodyTierChangeQueueStore tierChangeQueueStore;
	private final Supplier<MessageBodyObjectStore> bodyObjectStore;
	private final SystemConf sysconf;
	private final ItemValue<Server> server;

	public static class Factory implements ServerSideServiceProvider.IServerSideServiceFactory<IMessageBodyTierChange> {
		@Override
		public Class<IMessageBodyTierChange> factoryClass() {
			return IMessageBodyTierChange.class;
		}

		@Override
		public IMessageBodyTierChange instance(BmContext context, String... params) {
			if (params.length != 1) {
				throw new ServerFault("serverUid is required");
			}
			String serverUid = params[0];
			ItemValue<Server> server = Topology.get().datalocation(serverUid);
			if (server == null) {
				throw ServerFault.notFound("server " + serverUid + " not found");
			}
			DataSource ds = context.getMailboxDataSource(serverUid);
			if (ds == null) {
				throw ServerFault.notFound("datasource for serverUid (datalocation) " + serverUid + " not found");
			}
			return new MessageBodyTierChangeService(context, server, ds);
		}

	}

	public MessageBodyTierChangeService(BmContext context, ItemValue<Server> server, DataSource dataSource) {
		this.context = context;
		this.server = server;
		this.tierChangeQueueStore = new MessageBodyTierChangeQueueStore(dataSource);
		this.bodyObjectStore = Suppliers.memoize(() -> new MessageBodyObjectStore(context, server.uid));
		this.sysconf = LocalSysconfCache.get();
	}

	@Override
	public void createBody(MessageBody body) {
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		if (archiveKind == null || !archiveKind.supportsHsm()) {
			// HSM is disabled
			return;
		}

		Integer archiveSizeThreshold = sysconf.integerValue(SysConfKeys.archive_size_threshold.name());
		Integer archiveDays = sysconf.integerValue(SysConfKeys.archive_days.name(), 0);
		Instant changeAfter = null;
		Instant bodyCreated = body.created != null ? body.created.toInstant() : Instant.now();
		if (archiveDays > 0) {
			changeAfter = bodyCreated.plus(archiveDays, ChronoUnit.DAYS);
		}

		if (body.created != null && changeAfter != null && changeAfter.isBefore(Instant.now())) {
			// If the body was created in the past (using IMAP appendDate), then the body is
			// already on the correct storageTier. No need to move it "later".
			return;
		}

		if (body.size >= archiveSizeThreshold) {
			// Message is over the archive size threshold, archive it soon
			changeAfter = Instant.now().plus(15, ChronoUnit.DAYS);
		}

		if (changeAfter != null) {
			try {
				tierChangeQueueStore.insert(body.guid, changeAfter, Tier.SLOW);
			} catch (SQLException e) {
				logger.error("Unable to planify tier change for body {} to {}: {}", body.guid, changeAfter,
						e.getMessage());
			}
		}
	}

	@Override
	public Integer moveTier() {
		try {
			List<TierMove> tierMoves = tierChangeQueueStore.getMoves(IMessageBodyTierChange.TIER_CHANGES_PER_TICK);
			List<String> guidsMoved = bodyObjectStore.get().tierMove(tierMoves);

			tierChangeQueueStore.deleteMoves(guidsMoved);

			// Remove fully failed moves (exessive retries)
			List<TierMove> excessRetriesMoves = tierMoves.stream()
					.filter(tm -> tm.retries >= IMessageBodyTierChange.TIER_CHANGES_MAX_RETRIES).toList();
			if (!excessRetriesMoves.isEmpty()) {
				if (logger.isWarnEnabled()) {
					logger.warn("Tier moves with excessive retries will be discarded: {}",
							excessRetriesMoves.stream().map(TierMove::toString).collect(Collectors.joining(",")));
				}
				tierChangeQueueStore.deleteMoves(excessRetriesMoves.stream().map(tm -> tm.messageBodyGuid).toList());
			}

			// Error moving thoses retry with delay
			tierChangeQueueStore.retryDelayedMoves(
					tierMoves.stream().filter(tm -> tm.retries < IMessageBodyTierChange.TIER_CHANGES_MAX_RETRIES)
							.map(tm -> tm.messageBodyGuid).filter(guid -> !guidsMoved.contains(guid)).toList());
			return tierMoves.size();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void truncate() {
		try {
			tierChangeQueueStore.truncate();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public TaskRef requeueAllTierMoves() {
		ArchiveKind archiveKind = ArchiveKind.fromName(sysconf.stringValue(SysConfKeys.archive_kind.name()));
		if (archiveKind == null || !archiveKind.supportsHsm()) {
			return null;
		}
		Integer archiveDays = sysconf.integerValue(SysConfKeys.archive_days.name(), 0);
		var lastGuidWrapper = new Object() {
			byte[] lastGuid = { (byte) 0x00, (byte) 0x00 };
		};

		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(1, "Recalculating tierMoves for server " + server.displayName);
			long totalInserted = 0;
			try {
				logger.info("[{}] requeueing tierMoves", server.displayName);
				TierAddResult added;
				do {
					added = tierChangeQueueStore.rebuildTierMoves(10000, archiveDays, lastGuidWrapper.lastGuid);
					totalInserted += added.inserted();
					lastGuidWrapper.lastGuid = added.lastguid();
					if (logger.isInfoEnabled()) {
						// we are guid ordered and check position in the [ 0x0000 - 0xFFFF ] range
						long estimatedPercent = Math.round(
								(((lastGuidWrapper.lastGuid[0] & 0xff) << 8 | (lastGuidWrapper.lastGuid[1] & 0xff))
										/ 65535.0) * 100.0);
						logger.info("[{}] {} tierMoves requeued (estimated {}%) (last guid={})", server.displayName,
								added.inserted(), estimatedPercent, ByteBufUtil.hexDump(lastGuidWrapper.lastGuid));
					}
					Thread.sleep(1000);
				} while (added.inserted() > 0);
				logger.info("[{}] a total of {} tierMoves were requeued", server.displayName, totalInserted);
			} finally {
				monitor.end(true, "", "");
			}
		}));
	}

}

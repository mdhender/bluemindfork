/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.backup.continuous.mgmt.service.repair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.backup.continuous.mgmt.api.CheckAndRepairOptions;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DirEntryWithMailboxSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DirEntryWithMailboxSync.Scope;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DomainApis;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DomainKafkaState;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ExternalUserSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.GroupSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.MailshareSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ResourceSync;
import net.bluemind.core.backup.continuous.mgmt.service.impl.UserSync;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.core.utils.JsonUtils.ValueWriter;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.service.IInCoreGroup;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.user.api.User;
import net.bluemind.user.service.IInCoreUser;

public class StreamCheckAndRepair {
	private final IBackupStoreFactory store;
	private final IBackupStoreFactory liveStore;
	private final CheckAndRepairOptions options;
	private final IServerTaskMonitor monitor;
	private BmContext ctx;
	private static final Logger logger = LoggerFactory.getLogger(StreamCheckAndRepair.class);
	private final ValueWriter keyWriter;

	public StreamCheckAndRepair(BmContext ctx, IServerTaskMonitor monitor, CheckAndRepairOptions options) {
		this.ctx = ctx;
		this.options = options;
		this.monitor = monitor;
		keyWriter = JsonUtils.writer(RecordKey.class);
		store = DefaultBackupStore.store(InstallationId.getIdentifier(), Optional.of("presync"));
		liveStore = DefaultBackupStore.store(InstallationId.getIdentifier(), Optional.empty());
	}

	private record PartInfo(String stream, Integer partition, Long offset) {
	}

	private record SimpleItemValue(String uid) {
	}

	private static ValueReader<SimpleItemValue> reader = JsonUtils.reader(new TypeReference<SimpleItemValue>() {
	});

	public void checkAndRepair(Optional<ILiveStream> preSyncStream, ILiveStream stream) {
		// Premier check: est-ce qu'on a un container dont le owner
		// est un dirEntry qui n'existe pas encore ?
		Map<String, PartInfo> owners = new ConcurrentHashMap<>();
		Set<String> toRepairOwners = ConcurrentHashMap.newKeySet();
		Set<String> missingOwners = ConcurrentHashMap.newKeySet();

		monitorAndLog("on {} using presync: {}", stream, preSyncStream);

		preSyncStream.ifPresent(s -> s.subscribe(de -> {
			if (de.key.type.equals("dir") && de.key.operation.equals(Operation.CREATE.name())) {
				SimpleItemValue iv = reader.read(de.payload);
				owners.put(iv.uid, new PartInfo("presync", de.part, de.offset));
			}
		}));

		stream.subscribe(de -> {
			if (de.key.type.equals("dir") && de.key.operation.equals(Operation.CREATE.name())) {
				SimpleItemValue iv = reader.read(de.payload);
				owners.put(iv.uid, new PartInfo("prod", de.part, de.offset));
				missingOwners.remove(iv.uid);
				if (toRepairOwners.contains(iv.uid)) {
					monitorAndLog("[{}] found owner {} on {}", stream, iv.uid, owners.get(iv.uid));
				}
			}
			if (!de.key.operation.equals(Operation.DELETE.name()) && !de.key.owner.equals("system")
					&& (!owners.containsKey(de.key.owner) && !toRepairOwners.contains(de.key.owner))) {
				monitorAndLog("[{}] partition={} offset={} entry '{}' invalid: owner not found (yet)", stream, de.part,
						de.offset, de.key);
				toRepairOwners.add(de.key.owner);
				missingOwners.add(de.key.owner);
			}
		});

		IInCoreDirectory internalDirApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreDirectory.class, stream.domainUid());

		// Firstly, we need to address fullyMissingOwners, check the directory
		// If we can't find the owner, then parse the whole topic and remove
		// every single document using owner=entryUid

		List<String> toRemoveDirEntries = missingOwners.stream()
				.filter(dirEntryUid -> internalDirApi.get(dirEntryUid) == null).toList();
		toRepairOwners.removeAll(missingOwners);

		if (!toRemoveDirEntries.isEmpty() && options.removeObjectsMissingOwner) {
			/* Kill entries associated with the missing owners in the live stream */
			stream.subscribe(de -> {
				if (!de.key.operation.equals(Operation.DELETE.name()) && (toRemoveDirEntries.contains(de.key.owner))) {
					monitorAndLog("removing {} from {}", de.key, stream);
					deleteLive(stream.domainUid(), de.key).whenComplete((v, ex) -> {
						if (ex != null) {
							monitor.error("Failed to store delete operation {}: {}", de.key, ex.getMessage());
							logger.error("Failed to store delete operation {}: {}", de.key, ex.getMessage());
						}
					});
					// Need to join but no waity waity, how ?
				}
			});
			// Reclaim memory
			toRemoveDirEntries.clear();
		}

		IInCoreUser userApi = ctx.provider().instance(IInCoreUser.class, stream.domainUid());
		IMailshare msApi = ctx.provider().instance(IMailshare.class, stream.domainUid());
		IExternalUser euApi = ctx.provider().instance(IExternalUser.class, stream.domainUid());
		IResources rsApi = ctx.provider().instance(IResources.class, stream.domainUid());
		IInCoreGroup grpApi = ctx.provider().instance(IInCoreGroup.class, stream.domainUid());
		IDomains domainApi = ctx.provider().instance(IDomains.class);
		ItemValue<Domain> domain = domainApi.get(stream.domainUid());
		IContainers contsApi = ctx.provider().instance(IContainers.class);
		BaseContainerDescriptor dirContainer = contsApi.getLight(domain.uid);
		DomainApis domApi = new DomainApis(domain, //
				ctx.provider().instance(IMailboxes.class, stream.domainUid()), //
				ctx.provider().instance(IDirectory.class, stream.domainUid()));
		BackupSyncOptions opts = new BackupSyncOptions();
		DomainKafkaState domKafkaState = new DomainKafkaState(new ConcurrentHashMap<>());
		opts.skipArchived = false;

		toRepairOwners.forEach(dirEntryUid -> {
			DirEntryWithMailboxSync<User> userSync = new UserSync(ctx, opts, domKafkaState, userApi, domApi);
			DirEntryWithMailboxSync<Mailshare> msSync = new MailshareSync(ctx, opts, domKafkaState, msApi, domApi);
			DirEntryWithMailboxSync<Group> grpSync = new GroupSync(ctx, opts, grpApi, domApi, domKafkaState);
			DirEntryWithMailboxSync<ResourceDescriptor> resSync = new ResourceSync(ctx, opts, rsApi, domApi,
					domKafkaState);
			ExternalUserSync extSync = new ExternalUserSync(euApi, domApi);

			ItemValue<DirEntry> dirEntry = internalDirApi.get(dirEntryUid);
			if (dirEntry != null) {
				// This will make RecordKey to have a CREATED operation
				dirEntry.updated = dirEntry.created;

				switch (dirEntry.value.kind) {
				case USER:
					if (options.repair) {
						userSync.syncEntry(dirEntry, monitor, store, dirContainer, Scope.EntryOnly);
					} else {
						monitorAndLog("[dry] syncEntry USER {}", dirEntry);
					}
					break;
				case MAILSHARE:
					if (options.repair) {
						msSync.syncEntry(dirEntry, monitor, store, dirContainer, Scope.EntryOnly);
					} else {
						monitorAndLog("[dry] syncEntry MAILSHARE {}", dirEntry);
					}
					break;
				case RESOURCE:
					if (options.repair) {
						resSync.syncEntry(dirEntry, monitor, store, dirContainer, Scope.EntryOnly);
					} else {
						monitorAndLog("[dry] syncEntry RESOURCE {}", dirEntry);
					}
					break;
				case GROUP:
					if (options.repair) {
						grpSync.syncEntry(dirEntry, monitor, store, dirContainer, Scope.EntryOnly);
					} else {
						monitorAndLog("[dry] syncEntry GROUP {}", dirEntry);
					}
					break;
				case EXTERNALUSER:
					if (options.repair) {
						extSync.syncEntry(dirEntry, monitor, store, dirContainer, Scope.EntryOnly);
					} else {
						monitorAndLog("[dry] syncEntry EXTERNALUSER {}", dirEntry);
					}
					break;
				default:
					monitorAndLog("[FATAL] unable to repair owner={} dirEntry {} unsupported", dirEntryUid, dirEntry);
					break;
				}
			}
		});

	}

	public CompletableFuture<Void> deleteLive(String domainUid, RecordKey key) {
		BaseContainerDescriptor bcd = BaseContainerDescriptor.create(key.uid, key.uid, key.owner, key.type, domainUid,
				false);
		IBackupStore<Object> liveTarget = liveStore.forContainer(bcd);
		String partitionKey = key.owner;
		RecordKey invalidateCreates = new RecordKey(key.type, key.owner, key.uid, key.id, key.valueClass,
				Operation.CREATE.name());
		RecordKey invalidateUpdates = new RecordKey(key.type, key.owner, key.uid, key.id, key.valueClass,
				Operation.UPDATE.name());
		RecordKey invalidateDeletes = new RecordKey(key.type, key.owner, key.uid, key.id, key.valueClass,
				Operation.DELETE.name());
		CompletableFuture<Void> delProm = liveTarget.storeRaw(partitionKey, keyWriter.write(invalidateDeletes), null);
		CompletableFuture<Void> updProm = liveTarget.storeRaw(partitionKey, keyWriter.write(invalidateUpdates), null);
		CompletableFuture<Void> creProm = liveTarget.storeRaw(partitionKey, keyWriter.write(invalidateCreates), null);
		return CompletableFuture.allOf(delProm, updProm, creProm);
	}

	private void monitorAndLog(String format, Object... params) {
		this.monitor.log(format, params);
		logger.info(format, params);
	}

}

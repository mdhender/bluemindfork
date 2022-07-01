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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.RecordKey.Operation;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.backup.continuous.mgmt.service.impl.DirEntryWithMailboxSync.Scope;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;
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

public class DomainSync {

	private static final Logger logger = LoggerFactory.getLogger(DomainSync.class);
	private final BmContext ctx;
	private final ItemValue<Domain> domain;
	private final BackupSyncOptions opts;

	public DomainSync(BmContext ctx, ItemValue<Domain> d, BackupSyncOptions opts) {
		this.ctx = ctx;
		this.domain = d;
		this.opts = opts;
	}

	/**
	 * Compute what is in the <code>inBackup</code> stream to figure out which data
	 * is missing. Then we fetch changesets from containers up to the first version
	 * in backup.
	 * 
	 * @param target
	 * @param mon
	 * @param inBackup
	 */
	public void sync(IBackupStoreFactory target, IServerTaskMonitor mon, ILiveStream inBackup) {
		logger.info("Sync {}", domain);

		mon.begin(100, "Inspect current kafka stream...");

		DomainKafkaState domKafkaState = computeSyncBoundaries(inBackup);
		mon.progress(5, "container states loaded (" + domKafkaState + ")");

		IContainers contsApi = ctx.provider().instance(IContainers.class);
		ContainerDescriptor dirContainer = contsApi.get(domain.uid);
		IInCoreDirectory internalDirApi = ctx.provider().instance(IInCoreDirectory.class, domain.uid);
		// delta sync ?
		ContainerChangeset<ItemIdentifier> missingDirChanges = internalDirApi.fullChangeset();
		mon.log("DIR changeset c: " + missingDirChanges.created.size() + " u: " + missingDirChanges.updated.size()
				+ " d: " + missingDirChanges.deleted.size() + " v" + missingDirChanges.version);
		ContainerState kafkaState = domKafkaState.containerState(domain.uid);
		List<ItemIdentifier> missingItemVersions = Streams
				.stream(Iterables.concat(missingDirChanges.created, missingDirChanges.updated)).filter(iv -> {
					boolean inKafState = kafkaState.versions.contains(iv.version);
					if (inKafState) {
						mon.log("Skip dir entry " + iv.id + "v" + iv.version + " (uid " + iv.uid
								+ ") as version is known.");
					}
					return !inKafState;
				}).collect(Collectors.toList());

		mon.log("Filling " + domain.value.defaultAlias + " directory with " + missingItemVersions.size()
				+ " missing item(s)");

		fillDirectory(target, mon.subWork(5), domKafkaState, dirContainer, missingItemVersions, Scope.Entry);

		fillDirectory(target, mon.subWork(90), domKafkaState, dirContainer, missingItemVersions, Scope.Content);

	}

	private void fillDirectory(IBackupStoreFactory target, IServerTaskMonitor mon, DomainKafkaState domKafkaState,
			ContainerDescriptor dirContainer, List<ItemIdentifier> missingItemVersions, Scope scope) {
		IDirectory dirApi = ctx.provider().instance(IDirectory.class, domain.uid);
		IInCoreUser userApi = ctx.provider().instance(IInCoreUser.class, domain.uid);
		IMailshare msApi = ctx.provider().instance(IMailshare.class, domain.uid);
		IExternalUser euApi = ctx.provider().instance(IExternalUser.class, domain.uid);
		IResources rsApi = ctx.provider().instance(IResources.class, domain.uid);
		IInCoreGroup grpApi = ctx.provider().instance(IInCoreGroup.class, domain.uid);
		IMailboxes mboxApi = ctx.provider().instance(IMailboxes.class, domain.uid);

		DomainApis domApi = new DomainApis(domain, mboxApi, dirApi);

		DirEntryWithMailboxSync<User> userSync = new UserSync(ctx, opts, domKafkaState, userApi, domApi);
		DirEntryWithMailboxSync<Mailshare> msSync = new MailshareSync(ctx, opts, domKafkaState, msApi, domApi);
		DirEntryWithMailboxSync<Group> grpSync = new GroupSync(ctx, opts, grpApi, domApi);
		DirEntryWithMailboxSync<ResourceDescriptor> resSync = new ResourceSync(ctx, opts, rsApi, domApi);

		mon.begin(missingItemVersions.size(), "Processing " + missingItemVersions.size() + " directory entries");

		for (List<ItemIdentifier> partition : Lists.partition(missingItemVersions, 50)) {
			List<ItemValue<DirEntry>> items = dirApi
					.getMultiple(partition.stream().map(ii -> ii.uid).collect(Collectors.toList()));
			mon.log("Partition size: " + items.size());
			for (ItemValue<DirEntry> ivDir : items) {
				mon.log("Check entry " + ivDir);
				IServerTaskMonitor entryMon = mon.subWork(ivDir.displayName + " (" + ivDir.uid + ")", 1);
				if (ivDir.value.system) {
					logger.info("Skip system {} ", ivDir);
					entryMon.progress(1, "skipped");
					continue;
				}
				if (ivDir.value.archived && opts.skipArchived) {
					logger.info("Skip archived {} ", ivDir);
					entryMon.progress(1, "skipped");
					continue;
				}
				logger.info("Process {}", ivDir);
				ivDir.updated = ivDir.created;
				// create this one
				switch (ivDir.value.kind) {
				case DOMAIN:
					// ignore, created when processing orphans
					entryMon.progress(1, "skipped");
					break;
				case USER:
					userSync.syncEntry(ivDir, entryMon, target, dirContainer, scope);
					break;
				case MAILSHARE:
					msSync.syncEntry(ivDir, entryMon, target, dirContainer, scope);
					break;
				case RESOURCE:
					resSync.syncEntry(ivDir, entryMon, target, dirContainer, scope);
					break;
				case GROUP:
					grpSync.syncEntry(ivDir, entryMon, target, dirContainer, scope);
					break;
				case EXTERNALUSER:
					ItemValue<ExternalUser> fullEu = euApi.getComplete(ivDir.uid);
					ItemValue<VCard> vcardEu = dirApi.getVCard(ivDir.uid);
					ItemValue<DirEntryAndValue<ExternalUser>> deEu = ItemValue.create(ivDir,
							new DirEntryAndValue<ExternalUser>(ivDir.value, fullEu.value, vcardEu.value, null));
					IBackupStore<DirEntryAndValue<ExternalUser>> topicEu = target.forContainer(dirContainer);
					topicEu.store(deEu);
					break;
				case ADDRESSBOOK:
				case CALENDAR:
				case ORG_UNIT:
					entryMon.progress(1, "WARN skip kind " + ivDir.value.kind);
					break;
				default:
					throw new RuntimeException("unsupported kind " + ivDir.value.kind);
				}
			}
		}
	}

	private DomainKafkaState computeSyncBoundaries(ILiveStream domStream) {
		Map<String, ContainerState> lowestVersionPerContainer = new ConcurrentHashMap<>();

		domStream.subscribe(de -> {
			String cont = de.key.uid;
			if (Operation.valueOf(de.key.operation) != Operation.DELETE) {
				JsonObject parsedItem = new JsonObject(Buffer.buffer(de.payload));
				parsedItem.remove("value");
				long version = parsedItem.getLong("version", 0L);
				long itemid = parsedItem.getLong("internalId", 0L);
				synchronized (lowestVersionPerContainer) {
					ContainerState state = lowestVersionPerContainer.computeIfAbsent(cont, ContainerState::new);
					state.itemIds.add(Range.singleton(itemid));
					state.versions.add(Range.singleton(version));
				}
			}
		});
		return new DomainKafkaState(lowestVersionPerContainer);
	}
}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.restore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.restore.domains.DomainRestorationHandler;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.backup.continuous.restore.mbox.DefaultSdsStoreLoader;
import net.bluemind.core.backup.continuous.restore.mbox.ISdsStoreLoader;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreDomains;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreSysconf;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;

public class InstallFromBackupTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(InstallFromBackupTask.class);

	private final String installationId;
	private final IBackupStoreFactory backupStore;
	private final TopologyMapping topologyMapping;
	private final IServiceProvider target;
	private final Map<String, IResumeToken> processedStreams;
	private final ISdsStoreLoader sdsAccess;

	private ArrayList<IClonePhaseObserver> observers;

	public InstallFromBackupTask(String installationId, IBackupStoreFactory store, TopologyMapping map,
			IServiceProvider target) {
		this(installationId, store, map, new DefaultSdsStoreLoader(), target);
	}

	@VisibleForTesting
	public InstallFromBackupTask(String installationId, IBackupStoreFactory store, TopologyMapping map,
			ISdsStoreLoader sdsAccess, IServiceProvider target) {
		this.installationId = installationId;
		this.target = target;
		this.processedStreams = new HashMap<>();
		this.topologyMapping = map;
		this.backupStore = store;
		this.sdsAccess = sdsAccess;
		this.observers = new ArrayList<>();

	}

	public void registerObserver(IClonePhaseObserver obs) {
		observers.add(obs);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {

		monitor.begin(4, "Topology, domains then directories...");

		Path cloneStatePath = Paths.get("/etc/bm", "clone.state.json");

		ILiveBackupStreams streams = backupStore.forInstallation(installationId);
		ILiveStream orphansStream = streams.orphans();
		CloneState cloneState = new CloneState(cloneStatePath, orphansStream);

		ClonedOrphans orphans = cloneOrphans(monitor, orphansStream, cloneState);

		List<ILiveStream> domainStreams = streams.domains();
		ISdsSyncStore sdsStore = sdsAccess.forSysconf(orphans.sysconf);
		cloneDomains(monitor, domainStreams, cloneState, orphans, sdsStore);
	}

	private static class ClonedOrphans {

		public Map<String, ItemValue<Server>> topology;
		public Map<String, ItemValue<Domain>> domains;
		public SystemConf sysconf;

		public ClonedOrphans(Map<String, ItemValue<Server>> topology, Map<String, ItemValue<Domain>> domains,
				SystemConf sysconf) {
			this.topology = topology;
			this.domains = domains;
			this.sysconf = sysconf;
		}

	}

	public ClonedOrphans cloneOrphans(IServerTaskMonitor monitor, ILiveStream orphansStream, CloneState cloneState) {
		Map<String, List<DataElement>> orphansByType = new HashMap<>();
		IResumeToken prevState = cloneState.forTopic(orphansStream);
		IResumeToken orphansStreamIndex = orphansStream.subscribe(prevState, de -> {
			orphansByType.computeIfAbsent(de.key.type, key -> new ArrayList<>()).add(de);
		});

		Map<String, ItemValue<Server>> topology = new RestoreTopology(installationId, target, topologyMapping)
				.restore(monitor, orphansByType.getOrDefault("installation", new ArrayList<>()));
		System.err.println("topo is " + topology);

		Map<String, ItemValue<Domain>> domains = new RestoreDomains(installationId, target, topology.values())
				.restore(monitor, orphansByType.getOrDefault("domains", new ArrayList<>()));

		SystemConf sysconf = new RestoreSysconf(installationId, target).restore(monitor,
				orphansByType.getOrDefault("sysconf", new ArrayList<>()));

		recordProcessed(monitor, cloneState, orphansStream, orphansStreamIndex);
		orphansByType.clear();

		return new ClonedOrphans(topology, domains, sysconf);
	}

	public void cloneDomains(IServerTaskMonitor monitor, List<ILiveStream> domainStreams, CloneState cloneState,
			ClonedOrphans orphans, ISdsSyncStore sdsStore) {
		IServerTaskMonitor domainMonitor = monitor.subWork("domains", domainStreams.size());
		domainStreams.forEach(domainStream -> {
			ItemValue<Domain> domain = orphans.domains.get(domainStream.domainUid());
			domainMonitor.begin(orphans.domains.size(), "Working on domain " + domain.uid);

			IResumeToken domainPrevIndex = cloneState.forTopic(domainStream);
			IResumeToken domainStreamIndex = domainPrevIndex;

			try (RestoreState state = new RestoreState(domain.uid, orphans.topology)) {
				DomainRestorationHandler restoration = new DomainRestorationHandler(monitor, domain, target, observers,
						sdsStore, state);
				domainStreamIndex = domainStream.subscribe(restoration::handle);
			} catch (IOException e) {
				logger.error("unexpected error when closing", e);
				domainMonitor.end(false, "Fail to restore " + domain.uid + ": " + e.getMessage(), null);
			} finally {
				recordProcessed(domainMonitor, cloneState, domainStream, domainStreamIndex);
				domainMonitor.end(false, "Domain " + domain.uid + " fully restored", null);
			}
		});
	}

	private void recordProcessed(IServerTaskMonitor monitor, CloneState cloneState, ILiveStream stream,
			IResumeToken index) {
		monitor.log("Processed " + stream + " up to " + index);
		processedStreams.put(stream.domainUid(), index);
		cloneState.record(stream.fullName(), index).save();
	}

//	private List<ILiveStream> streams(Predicate<ILiveStream> filt) {
//		return avail.stream().filter(ls -> ls.installationId().equals(installationId) && filt.test(ls))
//				.collect(Collectors.toList());
//	}
//
//	private Collection<ILiveStream> raw(Predicate<ILiveStream> filt) {
//		return rawTopics.stream().filter(ls -> ls.installationId().equals(installationId) && filt.test(ls))
//				.collect(Collectors.toList());
//	}

//	private void restoreSystemConfiguration(IServerTaskMonitor monitor) {
//		Optional<ILiveStream> sysconfStream = avail.stream().filter(ls -> ls.type().equals("sysconf")).findAny();
//		Optional<SystemConf> conf = sysconfStream.map(ls -> {
//			AtomicReference<DataElement> de = new AtomicReference<>();
//			IResumeToken confToken = ls.subscribe(cloneState.forTopic(ls), d -> {
//				System.err.println("on sysconf " + d);
//				de.set(d);
//			});
//			recordProcessed(monitor, ls, confToken);
//			DataElement d = de.get();
//			if (d != null) {
//				ValueReader<ItemValue<SystemConf>> scReader = JsonUtils
//						.reader(new TypeReference<ItemValue<SystemConf>>() {
//						});
//				return scReader.read(new String(d.payload)).value;
//			} else {
//				monitor.log("No sysconf found.");
//				return null;
//			}
//		});
//		this.sdsStore = conf.map(sysconf -> {
//			ISystemConfiguration confApi = target.instance(ISystemConfiguration.class);
//			monitor.log("Restore system configuration...");
//
//			confApi.updateMutableValues(sysconf.values);
//			monitor.log("System config restored to " + sysconf.values);
//			return sdsAccess.forSysconf(sysconf);
//		}).orElseGet(() -> {
//			ISystemConfiguration confApi = target.instance(ISystemConfiguration.class);
//			SystemConf sysconf = confApi.getValues();
//			monitor.log("sysconf is missing, using existing one " + sysconf);
//			return sdsAccess.forSysconf(sysconf);
//		});
//	}

//	private Map<String, ItemValue<Domain>> processDomains(IServerTaskMonitor monitor, ILiveStream domainStream) {
//		IServer topoApi = target.instance(IServer.class, installationId);
//		monitor.log("Processing domains stream " + domainStream);
//		ValueReader<ItemValue<Domain>> domReader = JsonUtils.reader(new TypeReference<ItemValue<Domain>>() {
//		});
//		IDomains domApi = target.instance(IDomains.class);
//		Map<String, ItemValue<Domain>> domainsToHandle = new HashMap<>();
//		IResumeToken domIndex = domainStream.subscribe(cloneState.forTopic(domainStream), domDE -> {
//			String domJs = new String(domDE.payload);
//			ItemValue<Domain> dom = domReader.read(domJs);
//			if (dom.uid.equals("global.virt")) {
//				return;
//			}
//			ItemValue<Domain> known = domApi.get(dom.uid);
//			if (known != null) {
//				logger.info("UPDATE DOMAIN {}", dom);
//				System.err.println("Pre-update domain with " + new JsonObject(domJs).encodePrettily());
//				domApi.update(dom.uid, dom.value);
//			} else {
//				logger.info("CREATE DOMAIN {}", dom);
//				domApi.create(dom.uid, dom.value);
//				for (ItemValue<Server> iv : clonedTopology.values()) {
//					for (String tag : iv.value.tags) {
//						topoApi.assign(iv.uid, dom.uid, tag);
//					}
//					monitor.log("assign " + iv.uid + " to " + dom.uid);
//				}
//			}
//			domainsToHandle.put(dom.uid, dom);
//		});
//		recordProcessed(monitor, domainStream, domIndex);
//		monitor.progress(1, "Dealt with " + domainsToHandle.size() + " domain(s)");
//		return domainsToHandle;
//	}

//	private Map<String, ItemValue<Server>> processTopology(IServerTaskMonitor monitor,
//			ILiveStream installationsStream) {
//		monitor.log("Processing topology stream " + installationsStream);
//		ValueReader<ItemValue<Server>> topoReader = JsonUtils.reader(new TypeReference<ItemValue<Server>>() {
//		});
//		IServer topoApi = target.instance(IServer.class, installationId);
//		AtomicBoolean resetES = new AtomicBoolean();
//		List<ItemValue<Server>> touched = new LinkedList<>();
//		IResumeToken prevState = cloneState.forTopic(installationsStream);
//		IResumeToken topoIndex = installationsStream.subscribe(prevState, srvDE -> {
//			ItemValue<Server> srv = topoReader.read(new String(srvDE.payload));
//			TaskRef srvTask;
//			srv.value.ip = topologyMapping.ipAddressForUid(srv.uid, srv.value.ip);
//			ItemValue<Server> exist = topoApi.getComplete(srv.uid);
//			if (exist != null) {
//				logger.info("UPDATE SRV {}", srv);
//				srvTask = topoApi.update(srv.uid, srv.value);
//				if (srv.value.tags.contains("bm/es") && exist != null && !exist.value.tags.contains("bm/es")) {
//					resetES.set(true);
//				}
//			} else {
//				logger.info("CREATE SRV {}", srv);
//				srvTask = topoApi.create(srv.uid, srv.value);
//				if (srv.value.tags.contains("bm/es")) {
//					resetES.set(true);
//				}
//			}
//			touched.add(srv);
//			String wait = logStreamWait(srvTask);
//			monitor.log(srv.uid + ": " + wait);
//		});
//		if (resetES.get()) {
//			monitor.log("Reset ES indexes...");
//			IInstallation instApi = target.instance(IInstallation.class);
//			instApi.resetIndexes();
//		}
//		recordProcessed(monitor, installationsStream, topoIndex);
//		monitor.progress(1, "Dealt with topology");
//		List<ItemValue<Server>> exist = topoApi.allComplete();
//		return Optional.ofNullable(exist).orElse(touched).stream()
//				.collect(Collectors.toMap(iv -> iv.uid, iv -> iv, (iv1, iv2) -> iv2));
//	}

//	private static class JsDirEntry {
//		String domainUid;
//		String jsString;
//		JsonObject parsed;
//		Kind kind;
//	}
//
//	private void processAllDirectories(IServerTaskMonitor mon, ILiveStream dirStream) throws Exception {
//
//		Map<String, ItemValue<Domain>> domains = new HashMap<>();
//		Queue<JsDirEntry> entries = new LinkedList<>();
//		IResumeToken dirIndex = dirStream.subscribe(cloneState.forTopic(dirStream), de -> {
//			String jsString = new String(de.payload);
//			JsonObject parsed = new JsonObject(jsString);
//			JsDirEntry js = new JsDirEntry();
//			js.domainUid = de.key.uid;
//			js.parsed = parsed;
//			if (parsed.getJsonObject("value").containsKey("entry")) {
//				js.kind = BaseDirEntry.Kind
//						.valueOf(parsed.getJsonObject("value").getJsonObject("entry").getString("kind"));
//			}
//			js.jsString = jsString;
//			entries.add(js);
//		});
//		mon.begin(entries.size(), entries.size() + " to process");
//		UidDatalocMapping locMapping = new UidDatalocMapping();
//
//		observers.forEach(obs -> obs.beforeMailboxesPopulate(mon));
//
//		while (!entries.isEmpty()) {
//			processEntry(mon.subWork(1), domains, entries.poll(), locMapping);
//		}
//
//		recordProcessed(mon, dirStream, dirIndex);
//
//		processMailboxRecords(mon, locMapping);
//
//		mon.end(true, "Finished processing dir entries", "OK");
//
//	}
//
//	private void processMailboxRecords(IServerTaskMonitor mon, UidDatalocMapping locMapping) throws IOException {
//		mon.log("folders <-> location mapping has " + locMapping.size() + " entries");
//		Collection<ILiveStream> perDomainRecords = raw(ls -> ls.type().equals(IMailReplicaUids.MAILBOX_RECORDS));
//		for (ILiveStream allRecords : perDomainRecords) {
//			Path tmp = Files.createTempFile(allRecords.domainUid(), ".mapdb");
//			Files.deleteIfExists(tmp);
//			DB handlesBackingStore = DBMaker.fileDB(tmp.toFile().getAbsolutePath())//
//					.checksumHeaderBypass().fileMmapEnable()//
//					.fileMmapPreclearDisable() //
//					.cleanerHackEnable()//
//					.fileDeleteAfterClose().make();
//
//			Map<CyrusPartition, SyncClientOIO> cons = new HashMap<>();
//			ValueReader<ItemValue<MailboxRecord>> recReader = JsonUtils
//					.reader(new TypeReference<ItemValue<MailboxRecord>>() {
//					});
//
//			HTreeMap<String, Integer> known = handlesBackingStore.hashMap("bodies-" + allRecords.domainUid())
//					.keySerializer(Serializer.STRING_ASCII).valueSerializer(Serializer.INTEGER).createOrOpen();
//			Map<Replica, HTreeMap<Long, String>> toApply = new HashMap<>();
//			IResumeToken recState = allRecords.subscribeAll(cloneState.forTopic(allRecords), de -> {
//				String uniqueId = IMailReplicaUids.getUniqueId(de.key.uid);
//				Replica repl = locMapping.get(uniqueId);
//				if (repl == null) {
//					mon.log("No replica for " + de.key.uid);
//					locMapping.dump();
//					return;
//				}
//
//				HTreeMap<Long, String> uidToRecString = toApply.computeIfAbsent(repl,
//						k -> handlesBackingStore.hashMap("recs-" + repl.folder.uid)
//								.keySerializer(Serializer.LONG_PACKED).valueSerializer(Serializer.STRING_ASCII)
//								.createOrOpen());
//
//				SyncClientOIO sc = cons.computeIfAbsent(repl.part, cp -> {
//					try {
//						SyncClientOIO sync = new SyncClientOIO(
//								topologyMapping.ipAddressForUid(cp.serverUid, "127.0.0.1"), 2502);
//						sync.authenticate("admin0", Token.admin0());
//						return sync;
//					} catch (Exception e) {
//						throw new CloneException(e);
//					}
//				});
//				ItemValue<MailboxRecord> rec = recReader.read(new String(de.payload));
//				if (!known.containsKey(rec.value.messageBody)) {
//					MsgBodyTask body = new MsgBodyTask(sdsStore, sc, repl);
//					try {
//						int len = body.run(mon, rec.value.messageBody);
//						known.put(rec.value.messageBody, len);
//					} catch (Exception e) {
//						e.printStackTrace();
//						return;
//					}
//				}
//
//				// %(UID 1 MODSEQ 305 LAST_UPDATED 1619172582 FLAGS () INTERNALDATE 1619169573
//				// SIZE 54 GUID 3a6785fe8081d403c6721ae8637c0016db7963f8)
//
//				StringBuilder recordsBuffer = new StringBuilder();
//
//				recordsBuffer.append("%(");
//				recordsBuffer.append("UID ").append(rec.value.imapUid);
//				recordsBuffer.append(" MODSEQ ").append(rec.value.modSeq);
//				recordsBuffer.append(" LAST_UPDATED ").append(rec.value.lastUpdated.getTime() / 1000);
//				recordsBuffer.append(" FLAGS ()");
//				recordsBuffer.append(" INTERNALDATE ").append(rec.value.internalDate.getTime() / 1000);
//				recordsBuffer.append(" SIZE ").append(known.get(rec.value.messageBody));
//				recordsBuffer.append(" GUID " + rec.value.messageBody).append(")");
//
//				uidToRecString.put(rec.value.imapUid, recordsBuffer.toString());
//
//			});
//
//			for (Entry<Replica, HTreeMap<Long, String>> e : toApply.entrySet()) {
//				StringBuilder cmd = new StringBuilder(e.getKey().cmdPrefix);
//				HTreeMap<Long, String> recs = e.getValue();
//				AtomicInteger i = new AtomicInteger();
//
//				AtomicBoolean first = new AtomicBoolean(true);
//				recs.keySet().stream().sorted().forEach(uid -> {
//					String recStr = recs.get(uid);
//					if (recStr != null) {
//						if (!first.getAndSet(false)) {
//							cmd.append(' ');
//						}
//						cmd.append(recStr);
//						i.incrementAndGet();
//					}
//
//				});
//				cmd.append("))\r\n");
//
//				SyncClientOIO sync = cons.get(e.getKey().part);
//				String complete = cmd.toString();
//				String syncRes = sync.run(complete);
//				mon.log("Apply " + i.get() + " record(s) => " + syncRes);
//				if (!syncRes.startsWith("OK ")) {
//					mon.log(complete + " failed, abort !!!");
//					return;
//				}
//			}
//			handlesBackingStore.close();
//			recordProcessed(mon, allRecords, recState);
//		}
//
//	}
//
//	private void processEntry(IServerTaskMonitor mon, Map<String, ItemValue<Domain>> domains, JsDirEntry js,
//			UidDatalocMapping locMapping) {
//		ItemValue<Domain> domain = domains.computeIfAbsent(js.domainUid, uid -> {
//			IDomains domApi = target.instance(IDomains.class);
//			return domApi.get(uid);
//		});
//		if (js.kind == null) {
//
//			// should be a domain
//			ItemValue<DirEntry> entry = rawEntryReader.read(js.jsString);
//			System.err.println("on entry " + entry);
//			switch (entry.value.kind) {
//			case DOMAIN:
//				// skip
//				break;
//			case ADDRESSBOOK:
//				IAddressBooksMgmt bookApi = target.instance(IAddressBooksMgmt.class);
//				if (!entry.uid.equals("addressbook_" + domain.uid)) {
//					AddressBookDescriptor existing = bookApi.getComplete(entry.uid);
//					AddressBookDescriptor bookDesc = new AddressBookDescriptor();
//					bookDesc.owner = domain.uid;
//					bookDesc.domainUid = domain.uid;
//					bookDesc.name = entry.displayName;
//					bookDesc.orgUnitUid = entry.value.orgUnitUid;
//					if (existing != null) {
//						mon.log("Update addressbook " + bookDesc);
//						bookDesc.owner = existing.owner;
//						bookApi.update(entry.uid, bookDesc);
//					} else {
//						mon.log("Create addressbook " + bookDesc);
//						bookApi.create(entry.uid, bookDesc, false);
//					}
//				}
//				break;
//			case CALENDAR:
//				ICalendarsMgmt calApi = target.instance(ICalendarsMgmt.class);
//
//				CalendarDescriptor existing = calApi.getComplete(entry.uid);
//				CalendarDescriptor calDesc = new CalendarDescriptor();
//				calDesc.domainUid = domain.uid;
//				calDesc.owner = domain.uid;
//				calDesc.name = entry.displayName;
//				calDesc.orgUnitUid = entry.value.orgUnitUid;
//				if (existing != null) {
//					mon.log("Update calendar " + calDesc);
//					calDesc.owner = existing.owner;
//					calApi.update(entry.uid, calDesc);
//				} else {
//					mon.log("Create calendar " + calDesc);
//					calApi.create(entry.uid, calDesc);
//				}
//				break;
//			default:
//				// OK
//				break;
//			}
//		} else {
//			Kind kind = js.kind;
//
//			switch (kind) {
//			case USER:
//				ItemValue<FullDirEntry<User>> user = dirUserReader.read(js.jsString);
//				if (!user.value.value.system) {
//					IUser userApi = target.instance(IUser.class, domain.uid);
//					processUser(domain, mon, userApi, user, locMapping);
//				}
//				break;
//			case GROUP:
//				IGroup groupApi = target.instance(IGroup.class, domain.uid);
//				ItemValue<FullDirEntry<Group>> group = dirGroupReader.read(js.jsString);
//				// user & admin group have a generated uid
//				ItemValue<Group> existing = groupApi.byName(group.value.value.name);
//				if (existing != null) {
//					mon.log("Update group " + group.value.value);
//					groupApi.update(existing.uid, group.value.value);
//				} else {
//					mon.log("Create group " + group);
//
//					if (group.externalId != null) {
//						groupApi.createWithExtId(group.uid, group.externalId, group.value.value);
//					} else {
//						groupApi.create(group.uid, group.value.value);
//					}
//				}
//				break;
//			case MAILSHARE:
//				ItemValue<FullDirEntry<Mailshare>> share = dirMailshareReader.read(js.jsString);
//				if (!share.value.value.system) {
//					IMailshare shareApi = target.instance(IMailshare.class, domain.uid);
//					processMailshare(domain, mon, shareApi, share, locMapping);
//				}
//				break;
//			case RESOURCE:
//				ItemValue<FullDirEntry<ResourceDescriptor>> res = dirResourceReader.read(js.jsString);
//				if (!res.value.value.system) {
//					IResources resApi = target.instance(IResources.class, domain.uid);
//
//					ResourceDescriptor existingres = resApi.get(res.uid);
//					IResourceTypes typeApi = target.instance(IResourceTypes.class, domain.uid);
//					String wantedType = res.value.value.typeIdentifier;
//					ResourceTypeDescriptor knownType = typeApi.get(wantedType);
//					if (knownType == null) {
//						ResourceTypeDescriptor rtd = new ResourceTypeDescriptor();
//						rtd.label = "Auto-created " + wantedType;
//						rtd.properties = Collections.emptyList();
//						rtd.templates = Collections.emptyMap();
//						mon.log("Auto creating resource type " + rtd);
//						typeApi.create(wantedType, rtd);
//					}
//
//					if (existingres != null) {
//						mon.log("Update resource " + res.value.value);
//						resApi.update(res.uid, res.value.value);
//					} else {
//
//						mon.log("Create resource " + res.value.value);
//						resApi.create(res.uid, res.value.value);
//					}
//				}
//				break;
//			case EXTERNALUSER:
//				ItemValue<FullDirEntry<ExternalUser>> ext = dirExtUReader.read(js.jsString);
//				IExternalUser extApi = target.instance(IExternalUser.class, domain.uid);
//
//				ItemValue<ExternalUser> existingExt = extApi.getComplete(ext.uid);
//				if (existingExt != null) {
//					mon.log("Update external-user " + ext.value.value);
//					extApi.update(ext.uid, ext.value.value);
//				} else {
//					mon.log("Create external-user " + ext.value.value);
//					if (ext.externalId != null) {
//						extApi.createWithExtId(ext.uid, ext.externalId, ext.value.value);
//					} else {
//						extApi.create(ext.uid, ext.value.value);
//					}
//				}
//				break;
//			default:
//				System.err.println("Not supported kind " + kind + " yet");
//			}
//		}
//	}

//	private static class MembershipWithDomain {
//		ItemValue<GroupMembership> members;
//		String domainUid;
//	}
//
//	private void processMemberships(IServerTaskMonitor mon) throws Exception {
//		ILiveStream members = singleOrFail(ls -> ls.type().equals("memberships"), mon, "membership meta container");
//		ValueReader<ItemValue<GroupMembership>> membersReader = JsonUtils
//				.reader(new TypeReference<ItemValue<GroupMembership>>() {
//				});
//
//		Map<String, MembershipWithDomain> latest = new HashMap<>();
//
//		IResumeToken membersIndex = members.subscribe(cloneState.forTopic(members), de -> {
//			ItemValue<GroupMembership> ms = membersReader.read(new String(de.payload));
//			MembershipWithDomain withDom = new MembershipWithDomain();
//			withDom.domainUid = de.key.owner;
//			withDom.members = ms;
//			latest.put(ms.uid, withDom);
//		});
//		mon.begin(latest.size(), "Dealing with " + latest.size() + " membership(s)");
//		for (MembershipWithDomain withDom : latest.values()) {
//			ItemValue<GroupMembership> ms = withDom.members;
//			IGroup groupApi = target.instance(IGroup.class, withDom.domainUid);
//
//			mon.log("Saving " + ms.value.members.size() + " member(s) for group " + ms.uid);
//			List<Member> current = Optional.ofNullable(groupApi.getMembers(ms.uid)).orElse(Collections.emptyList());
//			groupApi.add(ms.uid, ms.value.members);
//			Map<String, Member> indexed = current.stream()
//					.collect(Collectors.toMap(m -> m.type.name() + ":" + m.uid, m -> m));
//			Map<String, Member> newIndexed = ms.value.members.stream()
//					.collect(Collectors.toMap(m -> m.type.name() + ":" + m.uid, m -> m));
//			HashSet<String> extra = new HashSet<>(indexed.keySet());
//			extra.removeAll(newIndexed.keySet());
//			List<Member> toRemove = extra.stream().map(indexed::get).collect(Collectors.toList());
//			if (!toRemove.isEmpty()) {
//				mon.log("Remove " + toRemove.size() + " extra member(s)");
//				groupApi.remove(ms.uid, toRemove);
//			}
//			mon.progress(1, ms.uid + " processed.");
//		}
//
//		recordProcessed(mon, members, membersIndex);
//		mon.end(true, "memberships processed.", "OK");
//	}

//	private void processMailshare(ItemValue<Domain> domain, IServerTaskMonitor mon, IMailshare shareApi,
//			ItemValue<FullDirEntry<Mailshare>> share, UidDatalocMapping locMapping) {
//		ItemValue<Mailshare> existingShare = shareApi.getComplete(share.uid);
//		if (existingShare != null) {
//			mon.log("Update mailshare " + share.value.value);
//			shareApi.update(share.uid, share.value.value);
//		} else {
//			mon.log("Create mailshare " + share.value.value);
//
//			ItemValue<Mailbox> mboxIv = ItemValue.create(share.uid, share.value.mailbox);
//
//			MboxFillTask tsk = new MboxFillTask(domain, mboxIv, cloneState, avail, clonedTopology, locMapping);
//			tsk.run(mon);
//
//			shareApi.create(share.uid, share.value.value);
//		}
//	}
//
//	private void processUser(ItemValue<Domain> dom, IServerTaskMonitor mon, IUser userApi,
//			ItemValue<FullDirEntry<User>> user, UidDatalocMapping locMapping) {
//		System.err.println(user.value.value.login + " hash: " + user.value.value.password);
//		ItemValue<User> existing = userApi.getComplete(user.uid);
//		ItemValue<Mailbox> mboxIv = ItemValue.create(user.uid, user.value.mailbox);
//
//		// Map<String, ItemValue<MailboxReplica>> folders;
//		if (existing != null) {
//			mon.log("Update user " + user.value.value);
//			userApi.update(user.uid, user.value.value);
//			MboxFillTask tsk = new MboxFillTask(dom, mboxIv, cloneState, avail, clonedTopology, locMapping);
//			tsk.run(mon);
//
//		} else {
//			mon.log("Create user " + user.value.value);
//
//			MboxFillTask tsk = new MboxFillTask(dom, mboxIv, cloneState, avail, clonedTopology, locMapping);
//			tsk.run(mon);
//
//			if (user.externalId != null) {
//				userApi.createWithExtId(user.uid, user.externalId, user.value.value);
//			} else {
//				userApi.create(user.uid, user.value.value);
//			}
//		}
//
//	}
//
//	private static class FullDirEntry<T> {
//		public DirEntry entry;
//		public VCard vcard;
//		public Mailbox mailbox;
//
//		public T value;
//
//		@Override
//		public String toString() {
//			return MoreObjects.toStringHelper("DE").add("entry", entry).add("vcard", vcard).add("mbox", mailbox)
//					.toString();
//		}
//	}

//	private ILiveStream singleOrFail(Predicate<ILiveStream> filter, IServerTaskMonitor mon, String log) {
//		List<ILiveStream> list = streams(filter);
//		if (list.size() != 1) {
//			String msg = String.format("Expected 1 stream for '%s', got %d out of %d", log, list.size(), avail.size());
//			mon.log("[" + log + "] Listing of available streams in " + backupStore + " follows...");
//			for (ILiveStream ls : avail) {
//				mon.log(" * " + ls);
//			}
//			for (ILiveStream ls : list) {
//				mon.log(" * MATCH " + ls);
//			}
//			throw new ServerFault(msg);
//		}
//		return list.get(0);
//	}

}

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
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;

import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailApiAnnotations;
import net.bluemind.config.Token;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.index.mail.Sudo;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ReplicationParentUidRepair implements IDirEntryRepairSupport {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationParentUidRepair.class);

	public static final MaintenanceOperation op = MaintenanceOperation.create("replication.parentUid",
			"Triggers cyrus replication on every IMAP folder");

	private final BmContext context;

	public ReplicationParentUidRepair(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
			return ImmutableSet.of(op);
		}
		return Collections.emptySet();
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind == Kind.USER || kind == Kind.MAILSHARE) {
			return ImmutableSet.of(new ReplicationParentUidMaintenance(context));
		}
		return Collections.emptySet();

	}

	public static class RepairFactory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new ReplicationParentUidRepair(context);
		}
	}

	private abstract static class MailboxWalk {
		protected final ItemValue<Mailbox> mbox;
		protected final String domainUid;
		protected final BmContext context;
		protected final Server srv;

		private MailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			this.srv = srv;
			this.context = context;
			this.mbox = mbox;
			this.domainUid = domainUid;
		}

		public static MailboxWalk create(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			if (mbox.value.type.sharedNs) {
				return new SharedMailboxWalk(context, mbox, domainUid, srv);
			} else {
				return new UserMailboxWalk(context, mbox, domainUid, srv);
			}
		}

		public abstract void folders(BiConsumer<StoreClient, List<ListInfo>> process);
	}

	public static final class UserMailboxWalk extends MailboxWalk {

		public UserMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process) {
			String login = mbox.value.name + "@" + domainUid;

			try (Sudo sudo = new Sudo(mbox.value.name, domainUid);
					StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId())) {
				if (!sc.login()) {
					logger.error("[{}] Fail to connect", mbox.value.name);
					return;
				}
				ListResult allFolders = sc.listAll();
				process.accept(sc, allFolders);
			}
		}
	}

	public static final class SharedMailboxWalk extends MailboxWalk {

		public SharedMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process) {
			try (StoreClient sc = new StoreClient(srv.address(), 1143, "admin0", Token.admin0())) {
				if (!sc.login()) {
					logger.error("Fail to connect as admin0 for {}", mbox.value.name);
					return;
				}
				List<ListInfo> mboxFoldersWithRoot = new LinkedList<>();
				ListInfo root = new ListInfo(mbox.value.name + "@" + domainUid, true);
				mboxFoldersWithRoot.add(root);
				ListResult shareChildren = sc.listSubFoldersMailbox(mbox.value.name + "@" + domainUid);
				mboxFoldersWithRoot.addAll(shareChildren);
				process.accept(sc, mboxFoldersWithRoot);
			}
		}
	}

	private static class ReplicationParentUidMaintenance extends InternalMaintenanceOperation {

		private final BmContext context;

		public ReplicationParentUidMaintenance(BmContext ctx) {
			super(op.identifier, null, IMailReplicaUids.REPAIR_SUBTREE_OP, 1);
			this.context = ctx;
		}

		@Override
		public void check(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {
			if (entry.archived) {
				return;
			}

			logger.info("Check replication parentUid {} {}", domainUid, entry);
		}

		@Override
		public void repair(String domainUid, DirEntry entry, DiagnosticReport report, IServerTaskMonitor monitor) {

			if (entry.archived) {
				logger.info("DirEntry is archived, skip it");
				return;
			}

			logger.info("Repair replication parentUid {} {}", domainUid, entry);

			IMailboxes iMailboxes = context.getServiceProvider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mbox = iMailboxes.getComplete(entry.entryUid);

			IServer iServer = context.getServiceProvider().instance(IServer.class, "default");
			ItemValue<Server> server = iServer.getComplete(entry.dataLocation);

			MailboxWalk moonWalk = MailboxWalk.create(context, mbox, domainUid, server.value);

			byte[] eml = ("From: noreply@" + domainUid + "\r\n").getBytes(StandardCharsets.US_ASCII);
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			fl.add(Flag.SEEN);

			AtomicBoolean repaired = new AtomicBoolean(false);
			Stopwatch chrono = Stopwatch.createStarted();
			moonWalk.folders((sc, allFolders) -> {
				Map<String, CompletableFuture<Void>> replAck = new ConcurrentHashMap<>();
				monitor.begin(allFolders.size(),
						"[" + entry.email + "] Dealing with " + allFolders.size() + " folders");
				MessageConsumer<String> cons = VertxPlatform.eventBus()
						.consumer(MailApiAnnotations.MSG_ANNOTATION_BUS_TOPIC);
				for (ListInfo f : allFolders) {
					String fn = f.getName();
					monitor.progress(1, "");
					if (!f.isSelectable() || fn.startsWith("Dossiers partagés/")
							|| fn.startsWith("Autres utilisateurs/")) {
						continue;
					}
					try {
						sc.select(fn);
					} catch (IMAPException e) {
						logger.info("Fail to select {} on mailbox {}", fn, mbox.value.name);
					}
					int addedUid = sc.append(fn, new ByteArrayInputStream(eml), fl,
							new GregorianCalendar(1970, Calendar.JANUARY, 1).getTime());
					String metaValue = new JsonObject().put("repl", UUID.randomUUID().toString()).encode();
					String tokenEnc = Base64.getUrlEncoder().encodeToString(metaValue.getBytes());
					replAck.put(tokenEnc, new CompletableFuture<>());
					cons.handler((Message<String> event) -> {
						String token = event.body();
						Optional.ofNullable(replAck.get(token)).ifPresent(p -> p.complete(null));
					});
					sc.setMessageAnnotation(addedUid, MailApiAnnotations.MSG_META, metaValue);
					sc.expunge();
				}

				CompletableFuture<?>[] allRepairs = replAck.values().stream().toArray(CompletableFuture[]::new);
				if (Boolean.getBoolean("core.repair.sync")) {
					try {
						CompletableFuture.allOf(allRepairs)
								.thenRun(() -> monitor.log(
										"[{}] repair completed at {} in {}ms & cyrus replication went well.",
										entry.email, new Date(), chrono.elapsed(TimeUnit.MILLISECONDS)))
								.get(5L * allFolders.size(), TimeUnit.SECONDS);
						repaired.set(true);
					} catch (InterruptedException e) {
						monitor.log("replication.parentUid repair failed to get all replication feedback", e);
						repaired.set(false);
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						monitor.log("replication.parentUid repair failed to get all replication feedback", e);
						repaired.set(false);
					}
				} else {
					repaired.set(true);
				}

				cons.unregister();

				if (repaired.get()) {
					monitor.end(true, "replication.parentUid", "200");
				} else {
					monitor.end(false, "replication.parentUid", "500");
				}
			});

		}
	}

}

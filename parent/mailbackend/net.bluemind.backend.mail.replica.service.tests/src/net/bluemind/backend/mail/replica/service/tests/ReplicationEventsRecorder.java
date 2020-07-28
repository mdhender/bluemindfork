/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.tests;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.backend.mail.api.events.MailEventAddresses;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ReplicationEventsRecorder {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationEventsRecorder.class);

	public static class Stats {
		public final LongAdder majorHierarchyChanges = new LongAdder();
		public final LongAdder minorHierarchyChanges = new LongAdder();
	}

	public static class Hierarchy {
		public long version = 0L;
		public long exactVersion = 0L;
		Set<String> mailboxUniqueIds = new HashSet<>();

		public String toString() {
			return MoreObjects.toStringHelper(getClass()).add("version", version)
					.add("mailboxUniqueIds", mailboxUniqueIds).toString();
		}
	}

	private final Vertx vertx;
	private final EventBus eb;
	private final Map<String, Stats> loginAtDomainStats;
	private final Map<String, Hierarchy> loginAtDomainHiearchies;

	public ReplicationEventsRecorder(Vertx vertx) {
		this.vertx = vertx;
		this.eb = this.vertx.eventBus();
		this.loginAtDomainStats = new ConcurrentHashMap<>();
		this.loginAtDomainHiearchies = new ConcurrentHashMap<>();
	}

	public Stats stats(String domainUid, String login) {
		return loginAtDomainStats.get(login + "@" + domainUid);
	}

	public Hierarchy hierarchy(String domainUid, String login) {
		return loginAtDomainHiearchies.get(login + "@" + domainUid);
	}

	public void recordUser(String domainUid, String login) {
		Stats stats = new Stats();
		loginAtDomainStats.put(login + "@" + domainUid, stats);
		Hierarchy hier = new Hierarchy();
		loginAtDomainHiearchies.put(login + "@" + domainUid, hier);
		String hierChange = MailEventAddresses.userMailboxHierarchyChanged(domainUid, login);

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IAuthentication authApi = prov.instance(IAuthentication.class);
		String latd = login + "@" + domainUid;
		Supplier<LoginResponse> logonProv = Suppliers.memoize(() -> authApi.su(latd));
		eb.consumer(hierChange, (Message<JsonObject> msg) -> {
			LoginResponse asUser = logonProv.get();
			logger.debug("asUser: {} {}", latd, asUser.status);
			if (asUser.status != LoginResponse.Status.Ok) {
				return;
			}
			IDbReplicatedMailboxes mailFoldersApi = prov.instance(IDbReplicatedMailboxes.class,
					domainUid.replace('.', '_'), "user." + login.replace('.', '^'));

			JsonObject body = msg.body();
			logger.info("HIER: {}", body.encode());
			boolean minor = body.getBoolean("minor", true);
			if (minor) {
				stats.minorHierarchyChanges.increment();
			} else {
				stats.majorHierarchyChanges.increment();
			}
			hier.exactVersion = body.getLong("version");
			if (!minor) {
				logger.info("MAJOR change, getting hierarchy changeset");
				ContainerChangeset<String> changes = mailFoldersApi.changeset(hier.version);
				hier.version = changes.version;

				for (String mboxUniqueId : Iterables.concat(changes.created, changes.updated)) {
					hier.mailboxUniqueIds.add(mboxUniqueId);
				}
				for (String mboxUniqueId : changes.deleted) {
					hier.mailboxUniqueIds.remove(mboxUniqueId);
				}
			}

		});
	}

}

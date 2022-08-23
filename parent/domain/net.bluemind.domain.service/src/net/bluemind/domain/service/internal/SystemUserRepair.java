/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.domain.service.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.RepairTaskMonitor;
import net.bluemind.domain.api.IDomainUids;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.api.ICacheMgmt;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SystemUserRepair implements IDirEntryRepairSupport {

	public static class Factory implements IDirEntryRepairSupport.Factory {
		@Override
		public IDirEntryRepairSupport create(BmContext context) {
			return new SystemUserRepair(context);
		}
	}

	public static final MaintenanceOperation systemUsers = MaintenanceOperation
			.create(IDomainUids.SYSTEM_USER_REPAIR_OP_ID, "System users");

	private final BmContext context;

	public SystemUserRepair(BmContext context) {
		this.context = context;
	}

	private static class SystemUserMaintenance extends InternalMaintenanceOperation {

		private static final String bmhiddensysadmin = "bmhiddensysadmin";
		private static final String admin0 = "admin0_global.virt";

		private final BmContext context;

		public SystemUserMaintenance(BmContext context) {
			super(systemUsers.identifier, null, null, 1);
			this.context = context;
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			checkSystemUser(domainUid, bmhiddensysadmin, () -> {
			}, monitor);

			if (domainUid.equals("global.virt")) {
				checkSystemUser(domainUid, admin0, () -> {
					ItemValue<User> admin0 = context.su().getServiceProvider().instance(IUser.class, "global.virt")
							.byLogin("admin0");
					if (admin0 != null) { // ad
						monitor.notify("UID of admin0 is {} instead of {}", admin0.uid, admin0);
					}
				}, monitor);
			}
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			checkSystemUser(domainUid, bmhiddensysadmin, () -> createBmHiddenSysadmin(domainUid), monitor);

			if (domainUid.equals("global.virt")) {
				checkAdmin0(domainUid, monitor);
				checkAdmin0Uid(monitor);
			}
		}

		private void checkAdmin0Uid(RepairTaskMonitor monitor) {
			ItemValue<User> admin0Item = context.su().getServiceProvider().instance(IUser.class, "global.virt")
					.byLogin("admin0");
			if (!admin0Item.uid.equals(admin0)) {
				monitor.notify("UID of admin0 is {} instead of {}", admin0Item.uid, admin0);
				String sql1 = String.format("update t_container_item set uid = '%s' where uid = '%s'", admin0,
						admin0Item.uid);
				String sql2 = String.format("update t_directory_entry set entry_uid = '%s' where entry_uid = '%s'",
						admin0, admin0Item.uid);
				try {
					try (Connection con = ServerSideServiceProvider.defaultDataSource.getConnection();
							Statement stm = con.createStatement()) {
						stm.executeUpdate(sql1);
						stm.executeUpdate(sql2);
					}
					context.su().getServiceProvider().instance(ICacheMgmt.class).flushCaches();
				} catch (SQLException e) {
					monitor.notify("Cannot fix admin0 uid: {}", e.getMessage());
				}
			}
		}

		private void checkAdmin0(String domainUid, RepairTaskMonitor monitor) {
			checkSystemUser(domainUid, admin0, () -> {
				ItemValue<User> admin0 = context.su().getServiceProvider().instance(IUser.class, "global.virt")
						.byLogin("admin0");
				if (admin0 == null) { // admin0 might exist having a wrong uid.
					createAdmin0();
				}
			}, monitor);
		}

		private void checkSystemUser(String domainUid, String userUid, Runnable op, RepairTaskMonitor monitor) {
			DirEntryQuery query = DirEntryQuery.filterEntryUid(userUid);
			query.systemFilter = false;
			query.hiddenFilter = false;
			ListResult<ItemValue<DirEntry>> dirEntry = context.su().provider().instance(IDirectory.class, domainUid)
					.search(query);
			boolean exists = false;
			if (dirEntry.total == 1) {
				IUser user = context.su().provider().instance(IUser.class, domainUid);
				User asUser = user.get(userUid);
				exists = asUser != null;
				if (!exists) {
					monitor.notify("User {} of domain {} does not exist", userUid, domainUid);
				}
			} else {
				monitor.notify("DirEntry {} of domain {} does not exist", userUid, domainUid);
			}
			if (!exists) {
				op.run();
			}
		}

		private void createBmHiddenSysadmin(String domainUid) {
			User user = new User();
			user.login = bmhiddensysadmin;
			user.password = UUID.randomUUID().toString();
			user.routing = Routing.none;
			user.hidden = true;
			user.system = true;
			VCard card = new VCard();
			card.identification.name = Name.create("System", null, null, null, null, null);
			user.contactInfos = card;
			context.su().provider().instance(IUser.class, domainUid).create(bmhiddensysadmin, user);
		}

		private void createAdmin0() {
			User admin0 = new User();
			admin0.login = "admin0";
			admin0.password = "admin";// NOSONAR
			admin0.routing = Mailbox.Routing.none;
			admin0.emails = ImmutableList.of(net.bluemind.core.api.Email.create("admin0@global.virt", true));
			VCard card = new VCard();
			card.identification.name = VCard.Identification.Name.create("admin0", "admin0", null, null, null, null);
			admin0.contactInfos = card;
			admin0.system = true;
			String uid = "admin0_global.virt";

			context.su().provider().instance(IUser.class, "global.virt").create(uid, admin0);
		}

	}

	@Override
	public Set<MaintenanceOperation> availableOperations(Kind kind) {
		if (kind != Kind.DOMAIN) {
			return Collections.emptySet();
		}
		return ImmutableSet.of(systemUsers);
	}

	@Override
	public Set<InternalMaintenanceOperation> ops(Kind kind) {
		if (kind != Kind.DOMAIN) {
			return Collections.emptySet();
		}
		return ImmutableSet.of(new SystemUserMaintenance(context));
	}

}

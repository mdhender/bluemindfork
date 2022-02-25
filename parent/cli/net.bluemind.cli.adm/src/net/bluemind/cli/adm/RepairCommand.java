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
package net.bluemind.cli.adm;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.NoopException;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.api.report.DiagnosticReport.State;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "repair", description = "Run repair maintenance operation")
public class RepairCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RepairCommand.class;
		}

	}

	@Option(names = "--dry", description = "Dry-run (run check instead of repair)")
	public boolean dry = false;

	@Option(names = "--ops", description = "Just include the (comma separated) ops, (eg. mailboxPostfixMaps,)")
	public String ops;

	@Option(names = "--unarchive", description = "\"true\" to temporarely unarchive/archive users and apply the repair op")
	public boolean unarchive = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IUser userService = null;
		ItemValue<User> userItem = null;

		boolean archived = unarchive && de.value.kind == Kind.USER && de.value.archived;
		try {
			if (archived) {
				ctx.info("User " + de.value.entryUid + " will be unarchived for repair op");
				userService = ctx.adminApi().instance(IUser.class, domainUid);
				userItem = userService.getComplete(de.value.entryUid);
				unarchive(userService, userItem);
			}

			repair(domainUid, de);
		} finally {
			if (archived && userService != null && userItem != null) {
				ctx.info("User " + de.value.entryUid + " will be archived");
				archive(userService, userItem);
			}
		}
	}

	private void repair(String domainUid, ItemValue<DirEntry> de) {
		IDirEntryMaintenance demService = ctx.adminApi().instance(IDirEntryMaintenance.class, domainUid, de.uid);
		Set<String> opsIds = demService.getAvailableOperations().stream().map(mo -> mo.identifier)
				.collect(Collectors.toSet());
		Set<String> filteredOps = opsIds;
		if (ops != null) {
			Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
			Set<String> toRun = Sets.newHashSet(splitter.split(ops));
			filteredOps = Sets.intersection(toRun, opsIds);

			if (filteredOps.isEmpty()) {
				throw new NoopException();
			}

			ctx.info("Selected ops: " + filteredOps);
		}

		TaskRef ref = dry ? demService.check(filteredOps) : demService.repair(filteredOps);
		TaskStatus status = Tasks.follow(ctx, ref, String.format("Failed to repair entry %s", de));
		if (!status.state.succeed) {
			DiagnosticReport report = JsonUtils.read(status.result, DiagnosticReport.class);
			report.entries.stream().filter(e -> e.state == State.KO).forEach(e -> ctx.error(e.toString()));
		}
	}

	private void unarchive(IUser userService, ItemValue<User> userItem) {
		userItem.value.archived = false;
		updateUser(userService, userItem);
	}

	private void archive(IUser userService, ItemValue<User> userItem) {
		userItem.value.archived = true;
		updateUser(userService, userItem);
	}

	private void updateUser(IUser userService, ItemValue<User> userItem) {
		userService.update(userItem.uid, userItem.value);
	}

	@Override
	public Kind[] getDirEntryKind() {
		return DirEntry.Kind.values();
	}
}
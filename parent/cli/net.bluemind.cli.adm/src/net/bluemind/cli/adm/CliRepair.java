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
package net.bluemind.cli.adm;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.directory.common.NoopException;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class CliRepair {
	protected final CliContext ctx;
	protected final boolean unarchive;
	protected final boolean dry;
	private final boolean verbose;
	protected Collection<String> askedRepairOps;

	protected Optional<ItemValue<User>> archiveUserOnClose = Optional.empty();
	protected String domainUid;
	protected ItemValue<DirEntry> dirEntry;

	private IUser userService;

	public CliRepair(CliContext ctx, String domainUid, ItemValue<DirEntry> dirEntry, boolean unarchive, boolean dry,
			boolean verbose) {
		this.ctx = ctx;
		this.unarchive = unarchive;
		this.dirEntry = dirEntry;
		this.domainUid = domainUid;
		this.dry = dry;
		this.verbose = verbose;

		userService = ctx.adminApi().instance(IUser.class, domainUid);
	}

	public void repair() {
		repair(null);
	}

	public void repair(String ops) {
		if (unarchive && dirEntry.value.kind == Kind.USER && dirEntry.value.archived) {
			ctx.info("User " + dirEntry.value.entryUid + " will be unarchived for repair op");
			archiveUserOnClose = Optional.ofNullable(userService.getComplete(dirEntry.value.entryUid));
			unarchive(userService, archiveUserOnClose);
		}
		doRepair(domainUid, dirEntry, ops);
	}

	private void doRepair(String domainUid, ItemValue<DirEntry> de, String ops) {
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

		String logId = (de.value.email != null && !de.value.email.isEmpty()) ? (de.value.email + " (" + de.uid + ")")
				: de.uid;

		RepairConfig config = new RepairConfig();
		config.dry = dry;
		config.logToCoreLog = !dry;
		config.verbose = verbose;
		config.opIdentifiers = filteredOps;
		TaskRef ref = demService.repair(config);
		Tasks.follow(ctx, ref, logId, String.format("Failed to repair entry %s", de));
	}

	private void unarchive(IUser userService, Optional<ItemValue<User>> ouserItem) {
		ouserItem.ifPresent(userItem -> {
			userItem.value.archived = false;
			updateUser(userService, userItem);
		});
	}

	private void archive(IUser userService, ItemValue<User> userItem) {
		// We absolutely need to retrieve the dirEntry again, because repair will modify
		// the direntry. We don't want to overwrite the old value!
		ItemValue<User> refreshedUser = userService.getComplete(userItem.uid);
		refreshedUser.value.archived = true;
		updateUser(userService, refreshedUser);
	}

	private void updateUser(IUser userService, ItemValue<User> userItem) {
		userService.update(userItem.uid, userItem.value);
	}

	public void close() {
		archiveUserOnClose.ifPresent(useriv -> {
			ctx.info("User {} will be archived", useriv.uid);
			archive(userService, useriv);
		});
	}
}

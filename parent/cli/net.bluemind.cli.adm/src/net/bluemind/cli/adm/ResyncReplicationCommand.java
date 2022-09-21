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

import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesRootMgmt;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import picocli.CommandLine.Command;

@Command(name = "full-replication-resync", description = "Force a resync of all IMAP folders")
public class ResyncReplicationCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ResyncReplicationCommand.class;
		}

	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IReplicatedMailboxesRootMgmt replMgmt = ctx.adminApi().instance(IReplicatedMailboxesRootMgmt.class, domainUid);
		if (!de.value.archived && !de.value.system) {
			TaskRef task = replMgmt.resync(de.value.entryUid);
			Tasks.followStream(ctx, "", task, true).join();
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new BaseDirEntry.Kind[] { BaseDirEntry.Kind.USER, BaseDirEntry.Kind.MAILSHARE };

	}
}
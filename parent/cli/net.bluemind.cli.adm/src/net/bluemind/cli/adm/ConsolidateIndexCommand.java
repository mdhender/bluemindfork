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

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.IMailboxMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "consolidateIndex", description = "Consolidate a mailbox index")
public class ConsolidateIndexCommand extends SingleOrDomainOperation {

	@Option(names = "--reset", description = "Reset the index (only the data belonging to the target)")
	public boolean reset = false;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ConsolidateIndexCommand.class;
		}
	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IMailboxMgmt imboxesMgmt = ctx.adminApi().instance(IMailboxMgmt.class, domainUid);
		String displayName = (de.value.email != null && !de.value.email.isEmpty())
				? (de.value.email + " (" + de.uid + ")")
				: de.uid;
		if (reset) {
			ctx.info("Resetting mailbox index of {}", displayName);
			TaskRef resetTask = imboxesMgmt.resetMailbox(de.uid);
			Tasks.follow(ctx, resetTask, displayName, String.format("Failed to reset mailbox index of entry %s", de));
		}
		TaskRef ref = imboxesMgmt.consolidateMailbox(de.uid);
		Tasks.follow(ctx, ref, displayName, String.format("Failed to consolidate mailbox index of entry %s", de));
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.GROUP, Kind.MAILSHARE, Kind.USER };
	}
}

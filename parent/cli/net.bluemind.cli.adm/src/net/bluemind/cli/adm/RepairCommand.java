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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
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

	@Option(names = "--verbose", description = "Print all logs")
	public boolean verbose = false;

	@Option(names = "--ops", description = "Just include the (comma separated) ops, (eg. mailboxPostfixMaps,)", completionCandidates = MaintenanceOpsCompletions.class)
	public String ops;

	@Option(names = "--unarchive", description = "\"true\" to temporarely unarchive/archive users and apply the repair op")
	public boolean unarchive = false;

	@Option(names = "--domain-only", description = "Only repair the domain entity")
	public boolean domainOnly = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		CliRepair clirepair = new CliRepair(ctx, domainUid, de, unarchive, dry, verbose);
		try {
			clirepair.repair(ops);
		} finally {
			clirepair.close();
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		if (domainOnly) {
			return new DirEntry.Kind[] { DirEntry.Kind.DOMAIN };
		} else {
			return DirEntry.Kind.values();
		}
	}
}
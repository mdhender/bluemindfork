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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.MaintenanceOperation;
import picocli.CommandLine.Command;

@Command(name = "ops", description = "List available maintenance operations")
public class RepairOpsCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return RepairOpsCommand.class;
		}

	}

	Map<Kind, List<MaintenanceOperation>> opsByKind = new EnumMap<>(Kind.class);

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IDirEntryMaintenance demService = ctx.adminApi().instance(IDirEntryMaintenance.class, domainUid, de.uid);
		List<MaintenanceOperation> ops = demService.getAvailableOperations();
		opsByKind.put(de.value.kind, ops);
	}

	@Override
	public void done() {
		opsByKind.forEach((kind, ops) -> {
			String[][] asTable = new String[ops.size()][2];
			int i = 0;
			for (MaintenanceOperation mo : ops) {
				asTable[i][0] = mo.identifier;
				asTable[i][1] = mo.description;
				i++;
			}
			ctx.info("Operation(s) available on type " + kind.name() + ":");
			ctx.info(AsciiTable.getTable(asTable));
		});

	}

	@Override
	public Kind[] getDirEntryKind() {
		return BaseDirEntry.Kind.values();
	}
}

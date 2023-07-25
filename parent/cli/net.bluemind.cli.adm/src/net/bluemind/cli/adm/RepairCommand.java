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

import java.util.Objects;
import java.util.Optional;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
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

	@Option(names = "--domain-only", description = "Only repair the domain entity")
	public boolean domainOnly = false;

	@Override
	public void run() {
		if (!domainOnly) {
			super.run();
			return;
		}

		if (scope.allDomains) {
			ctx.adminApi().instance(IDomains.class).all().forEach(this::repairDomain);
			return;
		}

		ItemValue<Domain> domain = null;
		if (Objects.nonNull(scope.target)) {
			domain = ctx.adminApi().instance(IDomains.class)
					.findByNameOrAliases(scope.target.contains("@") ? scope.target.split("@")[1] : scope.target);
		} else {
			domain = ctx.adminApi().instance(IDomains.class).get(scope.dirEntryUid);
		}
		if (domain == null) {
			ctx.warn("Domain for {} '{}' not found", Objects.nonNull(scope.target) ? "target" : "entry UID",
					Objects.nonNull(scope.target) ? scope.target : scope.dirEntryUid);
			return;
		}

		repairDomain(domain);
	}

	private void repairDomain(ItemValue<Domain> domain) {
		ListResult<ItemValue<DirEntry>> entries = ctx.adminApi().instance(IDirectory.class, domain.uid)
				.search(DirEntryQuery.entries(domain.uid));
		if (entries.total == 0) {
			ctx.warn("No directory entry found for domain {}", domain);
			return;
		}

		entries.values.forEach(entry -> new CliRepair(ctx, domain.uid, entry, dry, verbose).repair(ops));
	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		CliRepair clirepair = new CliRepair(ctx, domainUid, de, dry, verbose);
		clirepair.repair(ops);
	}

	@Override
	public Kind[] getDirEntryKind() {
		return DirEntry.Kind.values();
	}
}
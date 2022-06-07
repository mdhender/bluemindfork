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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cli.adm;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.domain.api.IDomainUids;

public class MaintenanceOpsCompletions implements Iterable<String> {
	private final Set<String> ops;

	public MaintenanceOpsCompletions() {
		ops = new LinkedHashSet<>();
		try {
			IDirEntryMaintenance demService = CliContext.get().adminApi().instance(IDirEntryMaintenance.class,
					IDomainUids.GLOBAL_VIRT, IDomainUids.GLOBAL_VIRT);
			demService.getAvailableOperations().stream().map(mo -> mo.identifier).forEach(ops::add);

			demService = CliContext.get().adminApi().instance(IDirEntryMaintenance.class, IDomainUids.GLOBAL_VIRT,
					"admin0_global.virt");
			demService.getAvailableOperations().stream().map(mo -> mo.identifier).forEach(ops::add);
		} catch (Exception e) {
			// some upgraded installations are missing entries for global.virt in
			// t_directory
		}
	}

	@Override
	public Iterator<String> iterator() {
		return ops.iterator();
	}

}

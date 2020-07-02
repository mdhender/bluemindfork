/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.directory.service.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.utils.DependencyResolver;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class DirEntryRepairSupports {
	private static final Logger logger = LoggerFactory.getLogger(DirEntryRepairSupports.class);

	private final List<IDirEntryRepairSupport> supports;

	public DirEntryRepairSupports(BmContext context) {
		List<IDirEntryRepairSupport.Factory> factories = RunnableExtensionLoader.builder()
				.pluginId("net.bluemind.directory").pointName("repairSupport").element("repairSupport")
				.implAttribute("factory").load();
		supports = factories.stream().map(f -> f.create(context)).collect(Collectors.toList());
	}

	public Set<MaintenanceOperation> availableOperations(DirEntry.Kind kind) {
		return supports.stream().map(s -> s.availableOperations(kind)).flatMap(s -> s.stream())
				.collect(Collectors.toSet());
	}

	public List<InternalMaintenanceOperation> ops(Set<String> filterIn, DirEntry.Kind kind) {
		List<InternalMaintenanceOperation> r = order(
				supports.stream().flatMap(s -> s.ops(kind).stream()).collect(Collectors.toList()));

		return r.stream().sequential()
				.filter(s -> filterIn == null || filterIn.isEmpty() || filterIn.contains(s.identifier))
				.collect(Collectors.toList());
	}

	public static List<InternalMaintenanceOperation> order(List<InternalMaintenanceOperation> toSort) {
		Map<String, Set<String>> deps = toSort.stream()
				.collect(Collectors.toMap(op -> op.identifier, op -> new HashSet<>()));
		for (InternalMaintenanceOperation op : toSort) {
			if (op.beforeOp != null && deps.containsKey(op.beforeOp)) {
				deps.get(op.beforeOp).add(op.identifier);
			}
			if (op.afterOp != null) {
				deps.get(op.identifier).add(op.afterOp);
			}
		}
		return DependencyResolver.sortByDependencies(toSort, op -> op.identifier, op -> deps.get(op.identifier));
	}

}

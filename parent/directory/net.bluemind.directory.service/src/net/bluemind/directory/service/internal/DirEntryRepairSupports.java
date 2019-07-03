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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
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

	private static class OpDeps {
		public InternalMaintenanceOperation op;
		public Set<String> deps = new HashSet<>();

		public OpDeps(InternalMaintenanceOperation op) {
			this.op = op;
		}
	}

	private static void visit(OpDeps op, Map<String, OpDeps> map, Set<String> temp, Set<String> permanent,
			List<OpDeps> res) {
		if (permanent.contains(op.op.identifier)) {
			return;
		}

		if (temp.contains(op.op.identifier)) {
			throw new ServerFault("circular dependency found", ErrorCode.INVALID_PARAMETER);
		}

		temp.add(op.op.identifier);
		op.deps.forEach(dep -> {
			if (!map.containsKey(dep)) {
				logger.warn(String.format("Unknow dependency %s for %s maintenance operation", dep, op.op.identifier));
				return;
			}

			visit(map.get(dep), map, temp, permanent, res);
		});
		permanent.add(op.op.identifier);
		res.add(op);
	}

	public static List<InternalMaintenanceOperation> order(List<InternalMaintenanceOperation> toSort) {
		Map<String, OpDeps> map = toSort.stream().map(OpDeps::new)
				.collect(Collectors.toMap(o -> o.op.identifier, o -> o));

		// transform before/after into simple dependencies ( a dependsOn b,c,d )
		map.values().forEach(o -> {
			if (o.op.beforeOp != null && map.get(o.op.beforeOp) != null) {
				map.get(o.op.beforeOp).deps.add(o.op.identifier);
			}

			if (o.op.afterOp != null) {
				o.deps.add(o.op.afterOp);
			}
		});

		// https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
		Set<String> temp = new HashSet<>();
		Set<String> permanent = new HashSet<>();
		List<OpDeps> res = new ArrayList<>(toSort.size());
		Collection<OpDeps> vCol = map.values();

		while (vCol.iterator().hasNext()) {
			OpDeps o = vCol.iterator().next();
			visit(o, map, temp, permanent, res);
			vCol = vCol.stream().sequential().filter(v -> !permanent.contains(o.op.identifier))
					.collect(Collectors.toList());
		}
		map.values().forEach(o -> {
			visit(o, map, temp, permanent, res);
		});

		return res.stream().sequential().map(o -> o.op).collect(Collectors.toList());
	}

}

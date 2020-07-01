/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.addressbook.service.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardChanges.ItemAdd;

public class GroupSortingByDependencies {

	static List<ItemAdd> sortByDependencies(List<ItemAdd> groups) {
		Map<String, GroupDependencies> groupDependecyInfo = groups.stream().map(GroupDependencies::new)
				.collect(Collectors.toMap(o -> o.card.uid, o -> o));

		groupDependecyInfo.values().forEach(groupDependency -> {
			for (VCard.Organizational.Member member : groupDependency.card.value.organizational.member) {
				// only add members of the same container (container == null)
				if (member.containerUid == null && member.itemUid != null) {
					groupDependency.deps.add(member.itemUid);
				}
			}
		});

		Set<String> temp = new HashSet<>();
		Set<String> permanent = new HashSet<>();
		List<GroupDependencies> finalList = new ArrayList<>(groups.size());
		Collection<GroupDependencies> dependencies = groupDependecyInfo.values();

		while (dependencies.iterator().hasNext()) {
			GroupDependencies dependency = dependencies.iterator().next();
			visit(dependency, groupDependecyInfo, temp, permanent, finalList);
			dependencies = dependencies.stream().sequential().filter(v -> !permanent.contains(dependency.card.uid)).collect(Collectors.toList());
		}
		groupDependecyInfo.values().forEach(o -> {
			visit(o, groupDependecyInfo, temp, permanent, finalList);
		});

		return finalList.stream().sequential().map(o -> o.card).collect(Collectors.toList());
	}

	private static void visit(GroupDependencies groupDep, Map<String, GroupDependencies> map, Set<String> temp,
			Set<String> permanent, List<GroupDependencies> res) {
		if (permanent.contains(groupDep.card.uid)) {
			return;
		}

		temp.add(groupDep.card.uid);
		groupDep.deps.forEach(dep -> {
			if (!map.containsKey(dep)) {
				return;
			}

			visit(map.get(dep), map, temp, permanent, res);
		});
		permanent.add(groupDep.card.uid);
		res.add(groupDep);
	}

	private static class GroupDependencies {
		public VCardChanges.ItemAdd card;
		public Set<String> deps = new HashSet<>();

		public GroupDependencies(VCardChanges.ItemAdd card) {
			this.card = card;
		}
	}

}

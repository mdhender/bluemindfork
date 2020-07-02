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
package net.bluemind.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class DependencyResolver {

	public static <T> List<T> sortByDependencies(List<T> elements, Function<T, String> idResolver,
			Function<T, Set<String>> dependenciesResolver) {
		Map<String, ElementDependency<T>> dependencyInfo = elements.stream()
				.map(g -> new ElementDependency<T>(g, idResolver.apply(g), dependenciesResolver.apply(g)))
				.collect(Collectors.toMap(o -> o.id, o -> o));

		Set<String> temp = new HashSet<>();
		Set<String> permanent = new HashSet<>();
		List<ElementDependency<T>> finalList = new ArrayList<>(elements.size());
		Collection<ElementDependency<T>> dependencies = dependencyInfo.values();

		while (dependencies.iterator().hasNext()) {
			ElementDependency<T> dependency = dependencies.iterator().next();
			visit(dependency, dependencyInfo, temp, permanent, finalList);
			dependencies = dependencies.stream().sequential().filter(v -> !permanent.contains(dependency.id))
					.collect(Collectors.toList());
		}
		dependencyInfo.values().forEach(o -> {
			visit(o, dependencyInfo, temp, permanent, finalList);
		});

		return finalList.stream().sequential().map(o -> o.value).collect(Collectors.toList());
	}

	private static <T> void visit(ElementDependency<T> dependency, Map<String, ElementDependency<T>> map,
			Set<String> temp, Set<String> permanent, List<ElementDependency<T>> res) {
		if (permanent.contains(dependency.id)) {
			return;
		}

		if (temp.contains(dependency.id)) {
			throw new ServerFault("circular dependency found", ErrorCode.INVALID_PARAMETER);
		}

		temp.add(dependency.id);
		dependency.deps.forEach(dep -> {
			if (!map.containsKey(dep)) {
				return;
			}
			visit(map.get(dep), map, temp, permanent, res);
		});
		permanent.add(dependency.id);
		res.add(dependency);
	}

	private static class ElementDependency<T> {
		public String id;
		public T value;
		public Set<String> deps = new HashSet<>();

		public ElementDependency(T value, String id, Set<String> dependencies) {
			this.id = id;
			this.value = value;
			this.deps = dependencies;
		}
	}

}

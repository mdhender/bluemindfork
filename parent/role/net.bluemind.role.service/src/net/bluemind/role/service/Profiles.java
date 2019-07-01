/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.role.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import net.bluemind.role.api.Profile;

public class Profiles {

	private static final Logger logger = LoggerFactory.getLogger(Profiles.class);
	public static final Map<String, Set<String>> profiles;

	static {
		profiles = loadProfiles();
	}

	public static Set<Profile> profiles(String locale) {
		Set<Profile> ret = new HashSet<>(10);
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.role.profile");
		if (point == null) {
			logger.error("point net.bluemind.role.profile not found");
			return ret;
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("profile")) {
					String id = e.getAttribute("id");
					String label = e.getAttribute("label", locale);
					Profile p = new Profile();
					p.id = id;
					p.label = label;
					ret.add(p);
				}
			}
		}

		return ret;

	}

	private static Map<String, Set<String>> loadProfiles() {
		Map<String, Set<String>> profiles = new HashMap<>();

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.role.profile");
		if (point == null) {
			logger.error("point net.bluemind.role.profile not found");
			return profiles;
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("profile")) {
					String id = e.getAttribute("id");
					Set<String> roles = null;
					if (profiles.containsKey(id)) {
						roles = profiles.get(id);
					} else {
						roles = new HashSet<>();
					}

					for (IConfigurationElement r : e.getChildren("role")) {
						roles.add(r.getAttribute("id"));
					}
					profiles.put(id, roles);
				}
			}
		}

		logger.info("Loaded " + profiles.size() + " profiles {}", profiles.keySet());
		return ImmutableMap.copyOf(profiles);

	}
}

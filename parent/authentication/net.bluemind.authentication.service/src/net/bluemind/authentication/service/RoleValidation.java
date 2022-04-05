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
package net.bluemind.authentication.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class RoleValidation {

	private static final Logger logger = LoggerFactory.getLogger(RoleValidation.class);

	private RoleValidation() {

	}

	private static Map<String, List<IRoleValidator>> validators = init();

	public static boolean validate(String domain, String role) {
		if (!validators.containsKey(role)) {
			return true;
		}

		boolean valid = true;
		for (IRoleValidator validator : validators.get(role)) {
			valid &= validator.valid(domain, role);
		}
		return valid;
	}

	private static Map<String, List<IRoleValidator>> init() {
		RunnableExtensionLoader<IRoleValidator> epLoader = new RunnableExtensionLoader<>();
		List<IRoleValidator> extensions = epLoader.loadExtensions("net.bluemind.authentication.service",
				"rolevalidation", "validator", "implementation");
		Map<String, List<IRoleValidator>> roleValidators = new HashMap<>();

		logger.debug("Found {} role validators", extensions.size());
		for (IRoleValidator validator : extensions) {
			List<String> supportedRoles = validator.supportedRoles();
			supportedRoles.forEach(role -> addIfNecessary(roleValidators, validator, role));
		}
		return roleValidators;
	}

	private static void addIfNecessary(Map<String, List<IRoleValidator>> roleValidators, IRoleValidator validator,
			String role) {
		List<IRoleValidator> currentValidators = roleValidators.getOrDefault(role, new ArrayList<>());
		currentValidators.add(validator);
		roleValidators.put(role, currentValidators);
	}

}

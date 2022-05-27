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
package net.bluemind.directory.api;

import java.util.Set;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class RepairConfig {

	public Set<String> opIdentifiers;

	public boolean dry;
	public boolean logToCoreLog;
	public boolean verbose;

	public static RepairConfig create(Set<String> opIdentifiers, boolean dry, boolean logToCore, boolean verbose) {
		RepairConfig config = new RepairConfig();
		config.opIdentifiers = opIdentifiers;
		config.dry = dry;
		config.logToCoreLog = logToCore;
		config.verbose = verbose;
		return config;
	}
}

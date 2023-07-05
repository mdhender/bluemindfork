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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.hornetq.client;

public class Shared {

	private Shared() {
	}

	/**
	 * Shared map<String,String> holding system configuration values
	 */
	public static final String MAP_SYSCONF = "system.configuration";

	/**
	 * Shared map<String,String> holding domain settings and properties values
	 */
	public static final String MAP_DOMAIN_SETTINGS = "domain.settings";

}

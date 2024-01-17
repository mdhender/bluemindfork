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
package net.bluemind.eas.command.provision;

import net.bluemind.eas.http.AuthorizedDeviceQuery;

public class Policies {

	public static String TEMPORARY_POLICY_KEY = "2147483647";
	public static String FINAL_POLICY_KEY = "4294967295";

	/**
	 * Called from activator to be sure a vertx classloader is not the first to
	 * load that
	 */
	public static void init() {
	}

	public static boolean hasValidPolicy(AuthorizedDeviceQuery query) {
		if ("Provision".equals(query.command())) {
			return true; // we are already doing the right thing
		}

		if (query.policyKey() == null) {
			return true;
		}

		return FINAL_POLICY_KEY.equals(Long.toString(query.policyKey()));
	}

}

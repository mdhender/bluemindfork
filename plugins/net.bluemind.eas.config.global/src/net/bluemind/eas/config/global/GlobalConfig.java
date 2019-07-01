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
package net.bluemind.eas.config.global;

import java.io.File;

public class GlobalConfig {

	private static final String root = "/etc/bm-eas/";

	public static int EAS_PORT = 8082;

	public static boolean DISABLE_POLICIES = new File(root, "disable.policies").exists();
	public static boolean DATA_IN_LOGS = new File(root, "data.in.logs").exists();
	public static boolean FAIL_ON_INVALID_REQUESTS = new File(root, "validate.requests").exists();

	public static boolean IMAP_WITH_TLS = false;

}

/*BEGIN LICENSE
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
package net.bluemind.system.iptables;

public class IptablesPath {
	public static final String IPTABLES_PATH = "/etc/init.d";
	public static final String IPTABLES_SCRIPT_NAME = "bm-iptables";
	public static final String IPTABLES_SCRIPT_PATH = IPTABLES_PATH + "/" + IPTABLES_SCRIPT_NAME;

	public static final String CHKCONFIG_PATH = "/etc/chkconfig.d";
	public static final String CHKCONFIG_IPTABLES_PATH = CHKCONFIG_PATH + "/" + IPTABLES_SCRIPT_NAME;
}

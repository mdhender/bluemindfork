/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.systemcheck.collect;

import java.io.File;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

public class NetworkHelper {
	private static final Logger logger = LoggerFactory.getLogger(NetworkHelper.class);

	public static String getMyIpAddress() {
		return fromHostnameCmd().orElseGet(NetworkHelper::fromNetworkInterface);
	}

	private static String fromNetworkInterface() {
		String ret = "127.0.0.1";

		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						ret = tmp;
					}
				}
			}
		} catch (SocketException e) {
		}

		return ret;
	}

	private static Optional<String> fromHostnameCmd() {
		// Debian
		String hostnameCmd = "/bin/hostname";
		if (!new File(hostnameCmd).exists()) {
			// RH/Ubuntu
			hostnameCmd = "/usr/bin/hostname";
			if (!new File(hostnameCmd).exists()) {
				return Optional.empty();
			}
		}

		try {
			CmdOutput ips = SystemHelper.cmdWithEnv(hostnameCmd + " -i", (Map<String, String>) null);
			Optional<String> found = ips.getOutput().stream().filter(ip -> ip != null)
					.filter(NetworkHelper::isIpAddress).filter(ip -> !ip.startsWith("127")).findFirst();

			found.ifPresent(ip -> logger.info("Found IP {} from hostname command", ip));
			return found;
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	public static boolean isIpAddress(final String ip) {
		return InetAddresses.isInetAddress(ip);
	}
}

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.sds.proxy.testhelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class ObjectStoreTestHelper {

	private ObjectStoreTestHelper() {

	}

	private static String getMyIpAddress() {
		String ret = "127.0.0.1";
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						return tmp;
					}
				}
			}
		} catch (SocketException e) {
		}
		return ret;
	}

	public static void setup(CyrusService cs, boolean restartCyrus) {
		ItemValue<Server> srv = cs.server();
		INodeClient nodeClient = NodeActivator.get(srv.value.address());
		try (InputStream in = ObjectStoreTestHelper.class.getClassLoader().getResourceAsStream("config/cyrus-hsm")) {
			nodeClient.writeFile("/etc/cyrus-hsm", in);
		} catch (IOException e) {
			throw new ServerFault(e);
		}

		if (restartCyrus) {
			cs.reload();
		}
	}

	public static void setupscality(CyrusService cs) {
		ItemValue<Server> srv = cs.server();
		INodeClient nodeClient = NodeActivator.get(srv.value.address());
		nodeClient.writeFile("/etc/bm/bm.ini",
				new ByteArrayInputStream(("[global]\nhz-member-address=" + getMyIpAddress() + "\n").getBytes()));
		nodeClient.writeFile("/etc/cyrus-hsm", new ByteArrayInputStream("archive_enabled: 0\n".getBytes()));
		cs.reload();
		cs.reloadSds();
	}

}

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

package net.bluemind.backend.systemconf.internal;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;

public class MyNetworksSanitizor implements ISystemConfigurationSanitizor {
	private static final String PARAMETER = "mynetworks";
	private static final String DEFAULT_VALUE = "127.0.0.0/8";

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		ParametersValidator.notNull(previous);

		if (!previous.values.containsKey(PARAMETER) && !modifications.containsKey(PARAMETER)) {
			modifications.put(PARAMETER, String.join(",", getDefaultValue()));
			return;
		}

		if (!modifications.containsKey(PARAMETER)) {
			return;
		}

		Set<String> parts = getSanitizedParts(modifications.get(PARAMETER));
		parts.remove(DEFAULT_VALUE);
		parts.removeAll(getKnownAddresses());

		parts.addAll(getDefaultValue());
		modifications.put(PARAMETER, Joiner.on(", ").join(parts));
	}

	public static Set<String> getSanitizedParts(String myNetworks) {
		HashSet<String> parts = new HashSet<String>();
		for (String part : myNetworks.split(" ")) {
			parts.addAll(Arrays.asList(part.split(",")));
		}

		HashSet<String> sanitizedParts = new HashSet<String>();
		for (String part : parts) {
			// part is: " xxx "
			part = part.trim();
			if (part.isEmpty()) {
				continue;
			}

			// part is: "!xxx"
			if (part.startsWith("!")) {
				part = part.substring(1);
			}

			sanitizedParts.add(part);
		}

		return sanitizedParts;
	}

	public static Set<String> getDefaultValue() {
		Set<String> network = new HashSet<>();
		network.add(DEFAULT_VALUE);
		network.addAll(getKnownAddresses());

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
						network.add(tmp + "/32");
					}
				}
			}
		} catch (SocketException e) {
		}

		return network;
	}

	private static Set<String> getKnownAddresses() {
		IServer serverApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				"default");

		List<ItemValue<Server>> servers = serverApi.allComplete();

		return servers.stream().map(s -> s.value.ip != null ? s.value.ip + "/32" : s.value.fqdn)
				.collect(Collectors.toSet());

	}
}

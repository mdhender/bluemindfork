/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.application.registration.model;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.application.registration.ApplicationRegistration;
import net.bluemind.system.application.registration.Store;

public class ApplicationInfo {

	public static ApplicationInfoModel createApplicationInfoModel(String installationId) {
		return new ApplicationInfoModel(System.getProperty("net.bluemind.property.product", "bm-crp"), getIpAddresses(),
				getMachineId(), installationId);
	}

	private static String getIpAddresses() {
		List<String> addrList = new ArrayList<>();
		try {
			for (Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces(); eni.hasMoreElements();) {
				final NetworkInterface ifc = eni.nextElement();
				if (ifc.isUp()) {
					for (Enumeration<InetAddress> ena = ifc.getInetAddresses(); ena.hasMoreElements();) {
						addrList.add(ena.nextElement().getHostAddress());
					}
				}
			}
		} catch (Exception e) {
			try {
				return Inet4Address.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				return "unknown";
			}
		}
		return addrList.stream().filter(ip -> !ip.startsWith("127.")).collect(Collectors.joining(",", "", ""));
	}

	private static String getMachineId() {
		try {
			return Files.readString(new File("/etc/machine-id").toPath()).replaceAll("\\r|\\n", "");
		} catch (Exception e) {
			return "unknown";
		}
	}

	public static void register(String installId) {
		ApplicationInfoModel info = createApplicationInfoModel(installId);
		Store productStore = new Store(info.product + info.machineId);
		if (productStore.isEnabled()) {
			VertxPlatform.eventBus().send(ApplicationRegistration.APPLICATION_REGISTRATION_INIT,
					JsonObject.mapFrom(info));
		}
	}

	public static void update(ApplicationInfoModel info) {
		JsonObject mapFrom = JsonObject.mapFrom(info);
		VertxPlatform.getVertx().eventBus().send(ApplicationRegistration.APPLICATION_REGISTRATION, mapFrom);
	}
}

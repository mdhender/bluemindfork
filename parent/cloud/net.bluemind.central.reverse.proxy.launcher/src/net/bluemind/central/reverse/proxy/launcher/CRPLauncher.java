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
package net.bluemind.central.reverse.proxy.launcher;

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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.central.reverse.proxy.ReverseProxyServer;
import net.bluemind.central.reverse.proxy.stream.DirEntriesStreamVerticleFactory;
import net.bluemind.system.application.registration.ApplicationInfo;
import net.bluemind.systemd.notify.Startup;

public class CRPLauncher implements IApplication {

	private static final Logger logger = LoggerFactory.getLogger(CRPLauncher.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		ReverseProxyServer crp = new ReverseProxyServer();
		crp.run();
		logger.info("CRP started");
		registerApplication();
		Startup.notifyReady();
		return IApplication.EXIT_OK;
	}

	private void registerApplication() {
		String machineId;
		try {
			machineId = Files.readString(new File("/etc/machine-id").toPath()).replaceAll("\\r|\\n", "");
		} catch (Exception e) {
			machineId = "unknown";
		}
		ApplicationInfo.register(
				new ApplicationInfo("bm-crp", getIpAddresses(), machineId,
						DirEntriesStreamVerticleFactory.config.getString("bm.crp.stream.forest-id")),
				() -> "running", () -> "");
	}

	private String getIpAddresses() {
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
			logger.warn("Cannot detect IP", e);
			try {
				return Inet4Address.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				return "unknown";
			}
		}
		return addrList.stream().filter(ip -> !ip.startsWith("127.")).collect(Collectors.joining(",", "", ""));
	}

	@Override
	public void stop() {
		logger.info("Application stopped.");
	}

}

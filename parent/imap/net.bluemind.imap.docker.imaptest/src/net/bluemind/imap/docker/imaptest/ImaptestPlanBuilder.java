/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.docker.imaptest;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Enumeration;
import java.util.List;

public class ImaptestPlanBuilder {

	private String targetHost;
	private int port = 1143;
	private String user;
	private String pass;
	private int clients;
	private Duration duration;
	private Integer select;
	private Integer logout;
	private Integer checkpoint;
	private boolean onlyLoginSelectLogout;

	public ImaptestPlanBuilder() {
		this.targetHost = getMyIpAddress();
		this.port = 1143;
		this.user = "tom@devenv.blue";
		this.pass = "tom";
		this.clients = 1;
		this.duration = Duration.ofSeconds(5);
		this.onlyLoginSelectLogout = false;
	}

	public DovecotImaptestRunner build() {
		String cmd = buildCommand();
		return new DovecotImaptestRunner(cmd);
	}

	public String buildCommand() {
		StringBuilder sb = new StringBuilder();
		sb.append("/root/imaptest seed=31032012 mbox=/root/dovecot-crlf");
		sb.append(" clients=" + clients);
		sb.append(" host=" + targetHost).append(" port=" + port);
		sb.append(" user=" + user).append(" pass=" + pass);
		if (duration != null) {
			sb.append(" secs=" + duration.toSeconds());
		}

		if (onlyLoginSelectLogout) {
			sb.append(" -");
		}
		if (select != null) {
			sb.append(" select=" + select);
		}
		if (logout != null) {
			sb.append(" logout=" + logout);
		}
		if (checkpoint != null) {
			sb.append(" checkpoint=" + checkpoint);
		}

		return sb.toString();
	}

	public ImaptestPlanBuilder user(String latd, String pass) {
		this.user = latd;
		this.pass = pass;
		return this;
	}

	public ImaptestPlanBuilder targetHost(String host) {
		this.targetHost = host;
		return this;
	}

	public ImaptestPlanBuilder port(int port) {
		this.port = port;
		return this;
	}

	public ImaptestPlanBuilder duration(Duration d) {
		this.duration = d;
		return this;
	}

	public ImaptestPlanBuilder clients(int i) {
		this.clients = i;
		return this;
	}

	public ImaptestPlanBuilder onlyLoginSelectLogout() {
		this.onlyLoginSelectLogout = true;
		return this;
	}

	/**
	 * 
	 * @param i how many select operation will be performed before exit
	 * @return
	 */
	public ImaptestPlanBuilder select(int i) {
		this.select = i;
		return this;
	}

	public ImaptestPlanBuilder logout(int i) {
		this.logout = i;
		return this;
	}

	public ImaptestPlanBuilder checkpoint(int i) {
		this.checkpoint = i;
		return this;
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
			// yeah yeah
		}
		return ret;
	}

	public static ImaptestPlanBuilder create() {
		return new ImaptestPlanBuilder();
	}

}

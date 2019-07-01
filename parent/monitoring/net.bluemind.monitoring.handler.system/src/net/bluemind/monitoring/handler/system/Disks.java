/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.handler.system;

import org.apache.commons.lang.StringUtils;

import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class Disks extends Service {

	private static Disks instance;

	private Disks() {
		super("system", "disks");
		this.endpoints.add("usage");
	}

	public static Disks getInstance() {
		if (Disks.instance == null) {
			Disks.instance = new Disks();
		}

		return Disks.instance;
	}

	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {

		switch (endpoint) {
		case "usage":
			return Disks.usage(server);
		}

		return null;
	}

	private static ServerInformation usage(Server server) {
		ServerInformation srvInfo = new ServerInformation(server, SystemHandler.BASE, "disks", "usage");

		Command c = new Command(SystemHandler.SCRIPTS_FOLDER + "disks.sh");

		boolean error = false;
		boolean warning = false;
		try {
			CommandExecutor.execCmdOnServer(server, c);
			for (FetchedData data : c.dataList) {
				String[] res = data.data.split(" ");
				String mount = res[0];
				String usageString = res[1];
				int usage = Integer.parseInt(StringUtils.chop(usageString));
				if (usage > 90) {
					error = true;
				} else if (usage > 80) {
					warning = true;
				}
				srvInfo.addData(new FetchedData(mount, usageString));
			}
		} catch (Exception e) {
			logger.error("Error retrieving server {} disk usage", server.address());
			srvInfo.addMessage(String.format("Unable to fetch disk usage for server %s", server.address()));
			srvInfo.setStatus(Status.UNKNOWN);
			return srvInfo;
		}

		if (error) {
			srvInfo.addMessage("server is too filed");
			srvInfo.setStatus(Status.KO);
		} else if (warning) {
			srvInfo.addMessage("server will soon be too filed");
			srvInfo.setStatus(Status.WARNING);
		} else {
			srvInfo.addMessage("server has enough free space");
			srvInfo.setStatus(Status.OK);
		}

		return srvInfo;
	}

}

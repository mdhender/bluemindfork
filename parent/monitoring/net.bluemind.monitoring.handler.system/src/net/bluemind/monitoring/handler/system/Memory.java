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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class Memory extends Service {

	private static Memory instance;

	private Memory() {
		super("system", "memory");
		this.endpoints.add("usage");
	}

	public static Memory getInstance() {
		if (Memory.instance == null) {
			Memory.instance = new Memory();
		}
		return Memory.instance;
	}

	/**
	 * Gets the memory usage of the server
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {
		switch (endpoint) {
		case "usage":
			return usage(server);

		}
		return null;
	}

	private static ServerInformation usage(Server server) {
		ServerInformation memInfo = new ServerInformation(server, SystemHandler.BASE, "memory", "usage");
		Command totalCmd, bufferedMemCmd, cachedMemCmd, freeMemCmd;

		totalCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "mem_get_info.sh " + "MemTotal");
		bufferedMemCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "mem_get_info.sh " + "Buffers");
		cachedMemCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "mem_get_info.sh " + "Cached");
		freeMemCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "mem_get_info.sh " + "MemFree");

		memInfo.commands.add(totalCmd);
		memInfo.commands.add(bufferedMemCmd);
		memInfo.commands.add(cachedMemCmd);
		memInfo.commands.add(freeMemCmd);

		memInfo.setStatus(Status.OK);

		try {
			CommandExecutor.execCmdOnServer(server, totalCmd);
			CommandExecutor.execCmdOnServer(server, bufferedMemCmd);
			CommandExecutor.execCmdOnServer(server, cachedMemCmd);
			CommandExecutor.execCmdOnServer(server, freeMemCmd);
		} catch (Exception e) {
			logger.error("Error retrieving server {} memory status", server.address());
			memInfo.addMessage(String.format("Unable to fetch memory status for server %s", server.address()));
			memInfo.setStatus(Status.UNKNOWN);
			return memInfo;
		}

		if (memInfo.hasData()) {

			float totalRam = Float.valueOf(totalCmd.rawData);
			float bufferedMem = Float.valueOf(bufferedMemCmd.rawData);
			float cachedMem = Float.valueOf(cachedMemCmd.rawData);
			float freeMem = Float.valueOf(freeMemCmd.rawData);
			float usedMem = totalRam - freeMem;

			float percentageUsed = (usedMem - bufferedMem - cachedMem) / totalRam * 100;

			if (percentageUsed > 90.0) {
				if (percentageUsed > 95.0) {
					memInfo.setStatus(Status.KO);
					memInfo.addMessage("More than 95% of the RAM is used");
				} else {
					memInfo.addMessage("Between 90% and 95% of the RAM is used");
					memInfo.setStatus(Status.WARNING);
				}
			}

			memInfo.addData(new FetchedData("UsedRam", String.valueOf(usedMem / 1024) + "MB"));
			memInfo.addData(new FetchedData("FreeRam", String.valueOf(freeMem / 1024) + "MB"));
			memInfo.addData(new FetchedData("TotalRam", String.valueOf(totalRam / 1024) + "MB"));
			memInfo.addData(new FetchedData("BufferedMem", String.valueOf(bufferedMem / 1024) + "MB"));
			memInfo.addData(new FetchedData("CachedMem", String.valueOf(cachedMem / 1024) + "MB"));
			memInfo.addData(new FetchedData("PercentageUsed", percentageUsed + "%"));
		} else {
			memInfo.setStatus(Status.KO);
			memInfo.addMessage("Unable to retrieve information about the RAM");
		}

		if (memInfo.status == Status.OK) {
			memInfo.addMessage("Less than 90% of the RAM is used");
		}

		return memInfo;
	}

}

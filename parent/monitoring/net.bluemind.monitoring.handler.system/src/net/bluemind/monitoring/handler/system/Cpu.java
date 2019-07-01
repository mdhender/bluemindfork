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
import net.bluemind.monitoring.service.util.Formatter;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class Cpu extends Service {

	private static Cpu instance;

	private Cpu() {
		super("system", "cpu");
		this.endpoints.add("load");
	}

	public static Cpu getInstance() {
		if (Cpu.instance == null) {
			Cpu.instance = new Cpu();
		}

		return Cpu.instance;
	}

	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {
		switch (endpoint) {
		case "load":
			return this.load(server);
		}
		return null;

	}

	private ServerInformation load(Server server) {

		ServerInformation cpuUsage = new ServerInformation(server, "system", "cpu", "load");

		cpuUsage.plugin = SystemHandler.BASE;
		cpuUsage.service = "cpu";

		Command loadAvgCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "cpu_loadavg.sh");
		Command nprocCmd = new Command(SystemHandler.SCRIPTS_FOLDER + "cpu_nproc.sh");
		String[] titles = { "LoadAvg5Minutes", "LoadAvg15Minutes" };

		try {
			fetchLoadAvg(cpuUsage, loadAvgCmd, titles);
			fetchNproc(server, cpuUsage, nprocCmd);

			Status status = Status.UNKNOWN;
			Float nproc = Float.parseFloat(nprocCmd.rawData);

			int count = 0;

			for (FetchedData data : loadAvgCmd.dataList.get(0).dataPieces) {
				Float loadAvg = Float.parseFloat(data.data);

				if (loadAvg <= 0.7 * nproc) {
					cpuUsage.addMessage("The CPU was working at less than 70% " + Cpu.when(count));
					status = (status.getValue() <= Status.OK.getValue() ? Status.OK : status);
				} else if (loadAvg > 0.7 * nproc && loadAvg <= nproc) {
					cpuUsage.addMessage("The CPU was working between 70% and 100% " + Cpu.when(count));
					status = (status.getValue() <= Status.WARNING.getValue() ? Status.WARNING : status);
				} else {
					cpuUsage.addMessage("The CPU had to work more than 100% " + Cpu.when(count));
					status = (status.getValue() <= Status.KO.getValue() ? Status.KO : status);
				}
				count++;
			}

			cpuUsage.setStatus(status);

		} catch (Exception e) {
			logger.error("Error retrieving server {} cpu status", server.address());
		}

		return cpuUsage;
	}

	private void fetchNproc(Server server, ServerInformation cpuUsage, Command nprocCmd) throws ServerFault {

		String nprocString;

		CommandExecutor.execCmdOnServer(server, nprocCmd);

		if (nprocCmd.hasDataList()) {
			nprocString = nprocCmd.rawData;
			cpuUsage.addData(new FetchedData("Nproc", nprocString));

			cpuUsage.commands.add(nprocCmd);
		} else {
			cpuUsage.setStatus(Status.KO);
			cpuUsage.addMessage("Unable to retrieve nproc value");
		}
	}

	private void fetchLoadAvg(ServerInformation cpuUsage, Command loadAvgCmd, String[] titles) throws ServerFault {
		CommandExecutor.execCmdOnServer(cpuUsage.server, loadAvgCmd);

		if (loadAvgCmd.hasDataList()) {
			Formatter.fillDataPieces(loadAvgCmd.dataList.get(0), 2, titles);

			for (FetchedData data : loadAvgCmd.dataList.get(0).dataPieces) {
				cpuUsage.addData(data);
			}

			cpuUsage.commands.add(loadAvgCmd);
		} else {
			cpuUsage.setStatus(Status.KO);
			cpuUsage.addMessage("Unable to retrieve the CPU load information");
		}
	}

	private static String when(int value) {
		switch (value) {
		case 0:
			return "the last 5 minutes";
		case 1:
			return "the last 15 minutes";
		default:
			return null;
		}
	}

}

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
package net.bluemind.monitoring.handler.services;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Formatter;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public abstract class AbstractService extends Service {

	private static final String CONN_TEST_OK = "The connection test was successful";
	private static final String CONN_TEST_KO = "The connection test failed";

	public AbstractService(String service, List<String> tags) {
		super("services", service, tags);

		this.endpoints.add("connection");
		this.endpoints.add("running");
	}

	public AbstractService(String service, String tag) {
		this(service, ImmutableList.of(tag));
	}

	public ServerInformation checkRunning(Server server) {
		BmService s = BmService.valueOf(service.toUpperCase());
		ServerInformation bmService = new ServerInformation(server, ServicesHandler.BASE, service, "running");
		Command servicePID, processInfoFromPID;

		try {
			// récupérer le PID du service
			servicePID = fetchBmServicePID(server, s.getFile());
			bmService.commands.add(servicePID);

			if (servicePID.hasDataList()) {
				int pid = Integer.valueOf(bmService.commands.get(0).dataList.get(0).data);
				bmService.addData(new FetchedData("ProcessID", "" + pid));

				// vérifier que le PID est dans les processus lancés
				processInfoFromPID = getProcessInfoFromPID(server, pid);
				bmService.commands.add(processInfoFromPID);

				if (processInfoFromPID.hasDataList()) {
					bmService.setStatus(Status.OK);
					bmService.dataList.add(new FetchedData("Running", Boolean.TRUE.toString()));

					checkHprofs(server, bmService, pid);

				} else {
					serviceNotFound(bmService, servicePID);
				}
			} else {
				bmService.setStatus(Status.WARNING);
				bmService.addMessage("Unable to read the PID of service " + service);
			}

			if (bmService.status == Status.OK) {
				bmService.addMessage(bmService.service + " runs normally");
			}

			return bmService;
		} catch (Exception e) {
			logger.error("[checkRunning] Error checking service {} on server {}", service, server.address(), e);
			return createException("running", server, CONN_TEST_KO);
		}

	}

	public ServerInformation checkConnection(Server server) {

		ServerInformation srvInfo = new ServerInformation(server, "services", service, "connection");
		Command cmd = new Command(ServicesHandler.SCRIPTS_FOLDER + service + "_connection.sh");

		try {
			int exitCode = CommandExecutor.execCmdOnServer(server, cmd);
			if (exitCode == 0) {
				srvInfo.setStatus(Status.OK);
				srvInfo.addMessage(AbstractService.CONN_TEST_OK);
			} else {
				srvInfo.setStatus(Status.KO);
				srvInfo.addMessage(cmd.rawData);
			}

		} catch (ServerFault e) {
			logger.error("[checkConnection] Error checking service {} on server {}", service, server.address(), e);
			return createException("connection", server, CONN_TEST_KO);
		}
		return srvInfo;
	}

	protected ServerInformation createException(String endpoint, Server server, String exceptionMsg) {
		ServerInformation srvInfo = new ServerInformation(server, plugin, service, endpoint);
		srvInfo.setStatus(Status.KO);
		srvInfo.addMessage(exceptionMsg);
		return srvInfo;
	}

	private Command checkRunningProcessHprof(Server server, int pid) throws ServerFault {

		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "running_proc_hprof.sh " + pid);

		CommandExecutor.execCmdOnServer(server, c);

		return c;

	}

	private Command fetchBmServicePID(Server server, String service) throws ServerFault {
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "bmservices_pid.sh " + service);

		CommandExecutor.execCmdOnServer(server, c);

		return c;
	}

	private Command getProcessInfoFromPID(Server server, int pid) throws ServerFault {
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "pid_to_proc_info.sh " + pid);
		String[] titles = { "PID", "%CPU", "%MEM", "START" };

		CommandExecutor.execCmdOnServer(server, c);

		if (c.hasDataList()) {
			if (c.dataList.size() > 0) {
				Formatter.fillDataPieces(c.dataList.get(0), 4, titles);
			}
		}

		return c;
	}

	private void serviceNotFound(ServerInformation bmService, Command servicePID) {
		bmService.setStatus(Status.KO);
		bmService.addMessage("Unable to find service " + service + " with PID " + servicePID.rawData);
		// on renvoie une data liste vide
		bmService.commands.get(1).dataList = null;
		// rajouter à la data list que le service ne tourne pas
		bmService.dataList.add(new FetchedData("Running", Boolean.FALSE.toString()));
	}

	private void checkHprofs(Server server, ServerInformation bmService, int pid) throws ServerFault {
		Command runningProcessHprof;
		// vérifier hprofs
		runningProcessHprof = checkRunningProcessHprof(server, pid);
		bmService.commands.add(runningProcessHprof);

		// hprof trouvé
		if (runningProcessHprof.hasDataList()) {
			bmService.setStatus(Status.KO);
			bmService.addData(new FetchedData("FoundHprof", Boolean.TRUE.toString()));
			bmService.addMessage("Hprofs were found " + service + " must be restarted");
		} else {
			bmService.addData(new FetchedData("FoundHprof", Boolean.FALSE.toString()));
		}
	}

	@Override
	public ServerInformation getServerInfo(Server server, String method) {
		switch (method) {
		case "connection":
			return checkConnection(server);
		case "running":
			return checkRunning(server);
		}

		return getSpecificServerInfo(server, method);
	}

	protected abstract ServerInformation getSpecificServerInfo(Server server, String method);

}

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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Formatter;
import net.bluemind.server.api.Server;

public class Cyrus extends AbstractService {
	
	public Cyrus() {
		super(BmService.CYRUS.toString(), "mail/imap");
		this.endpoints.add("processes");

	}
	
	public ServerInformation getImapProcesses(Server server) throws ServerFault {
		ServerInformation imapInfo = new ServerInformation(server, ServicesHandler.BASE, "cyrus", "processes");

		Command imapActiveProc = new Command(ServicesHandler.SCRIPTS_FOLDER + "imap_active_proc.sh");
		Command imapMaxProc = new Command(ServicesHandler.SCRIPTS_FOLDER + "imap_max_proc.sh");

		// the command to fetch the number of imap processes
		imapInfo.commands.add(imapActiveProc);
		// the command to fetch the maximum number of processes that can be
		// running at the same time
		imapInfo.commands.add(imapMaxProc);

		CommandExecutor.execCmdOnServer(server, imapActiveProc);
		CommandExecutor.execCmdOnServer(server, imapMaxProc);

		if (imapInfo.hasData()) {
			String nbProcs = imapActiveProc.dataList.get(0).data;
			String maxChild = imapMaxProc.dataList.get(0).data;

			// rajouter à l'information le nombre de processes qui tournent et
			// le nombre max de child IMAP
			imapInfo.addData(new FetchedData("ImapProcessCount", nbProcs));
			imapInfo.addData(new FetchedData("ImapMaxChild", maxChild));

			double percent = Integer.valueOf(Formatter.getMatches(nbProcs, "[0-9]+")) * 100.0 / Integer.valueOf(Formatter.getMatches(maxChild, "[0-9]+"));
			
			if (percent < 85d) {
				imapInfo.setStatus(Status.OK);
				imapInfo.addMessage("Number of IMAP connection is good");
			} else {
				imapInfo.setStatus(Status.WARNING);
				imapInfo.addMessage("MAX number of IMAP process is too low.");
			}

			// pas besoin de garder les pid
			imapInfo.commands.get(0).dataList = null;
		} else {
			imapInfo.setStatus(Status.KO);
			imapInfo.addMessage("Couldn't fetch IMAP info");
		}

		return imapInfo;
	}

	@Override
	public ServerInformation getSpecificServerInfo(Server server, String method) {
		switch (method) {
		case "processes":
			return getImapProcesses(server);
		}
		return null;
	}

}

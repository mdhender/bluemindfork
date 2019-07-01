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

import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Formatter;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class Leaks extends Service {

	private static Leaks instance;

	private Leaks() {
		super("system", "leaks");
		this.endpoints.add("check");
	}

	public static Leaks getInstance() {
		if (Leaks.instance == null) {
			Leaks.instance = new Leaks();
		}

		return Leaks.instance;
	}

	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {
		switch (endpoint) {
		case "check":
			return check(server);
		}
		return null;
	}

	private static ServerInformation check(Server server) {
		ServerInformation bmLeaks = new ServerInformation(server, SystemHandler.BASE, "leaks", "check");
		Command c = new Command(SystemHandler.SCRIPTS_FOLDER + "bmleaks.sh");

		bmLeaks.commands.add(c);

		try {
			CommandExecutor.execCmdOnServer(server, c);
		} catch (Exception e) {
			logger.error("Error retrieving server {} leaks status", server.address());
			bmLeaks.addMessage(String.format("Unable to fetch leaks status for server %s", server.address()));
			bmLeaks.setStatus(Status.UNKNOWN);
			return bmLeaks;
		}

		if (bmLeaks.commands.get(0).dataList.size() == 0) {
			bmLeaks.setStatus(Status.OK);
		} else {
			bmLeaks.setStatus(Status.KO);

			int i = 1;

			// décomposer en date time
			for (FetchedData dataPiece : bmLeaks.commands.get(0).dataList) {
				String rawDateTime = Formatter.getMatches(dataPiece.data, "[0-9]+-[0-9]+");

				// décomposer ensuite
				dataPiece.addDataPiece(new FetchedData("year", rawDateTime.substring(0, 4)));
				dataPiece.addDataPiece(new FetchedData("month", rawDateTime.substring(4, 6)));
				dataPiece.addDataPiece(new FetchedData("day", rawDateTime.substring(6, 8)));
				dataPiece.addDataPiece(new FetchedData("hour", rawDateTime.substring(9, 11)));
				dataPiece.addDataPiece(new FetchedData("minute", rawDateTime.substring(11, 13)));
				dataPiece.addDataPiece(new FetchedData("second", rawDateTime.substring(13, 15)));

				bmLeaks.addMessage("A leak occurred " + dataPiece.getDataPieceByTitle("day").data + "/"
						+ dataPiece.getDataPieceByTitle("month").data + "/" + dataPiece.getDataPieceByTitle("year").data
						+ " à " + dataPiece.getDataPieceByTitle("hour").data + ":"
						+ dataPiece.getDataPieceByTitle("minute").data + ":"
						+ dataPiece.getDataPieceByTitle("second").data);

				bmLeaks.addData(new FetchedData("BmLeak" + i, dataPiece.data));

				i++;

			}
		}

		if (bmLeaks.status == Status.OK) {
			bmLeaks.addMessage("No leak was found");
		}

		bmLeaks.addData(new FetchedData("BmLeaksFound", String.valueOf(bmLeaks.commands.get(0).dataList.size())));

		return bmLeaks;
	}

}

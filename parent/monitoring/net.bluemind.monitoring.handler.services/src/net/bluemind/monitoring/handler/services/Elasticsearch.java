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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Formatter;
import net.bluemind.server.api.Server;

public class Elasticsearch extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(Elasticsearch.class);

	public Elasticsearch() {
		super(BmService.ELASTICSEARCH.toString(), "bm/es");
		this.endpoints.add("health");
		this.endpoints.add("filedesc");
		this.endpoints.add("memory");

	}

	@Override
	public ServerInformation getSpecificServerInfo(Server server, String method) {
		switch (method) {
			case "health":
				return checkHealth(server);
			case "filedesc":
				return checkFileDesc(server);
			case "memory":
				return checkMemory(server);
		}
		return null;
	}
	
	private ServerInformation checkHealth(Server server) {
		ServerInformation serverInfo = new ServerInformation(server, this.plugin, this.service, "health");
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "elasticsearch_health.sh " + server.ip);

		try {
			CommandExecutor.execCmdOnServer(server, c);
			FetchedData health = c.dataList.get(0);
			health.title = "health";
			
			serverInfo.addData(health);
			
			switch(health.data) {
				case "green":
				case "yellow":
					serverInfo.setStatus(Status.OK);
					serverInfo.addMessage("Elasticsearch cluster is up");
					break;
				case "red":
					serverInfo.setStatus(Status.KO);
					serverInfo.addMessage("Elasticsearch cluster is down");
					break;
				default:
					serverInfo.setStatus(Status.WARNING);
					serverInfo.addMessage("Elasticsearch cluster state is unknown");
					break;
			}

		} catch (Exception e) {
			serverInfo.addMessage("Unable to fetch information about Elasticsearch's status");
			logger.error("Unable to get information for {}", service, e);
		}

		return serverInfo;
	}

	private ServerInformation checkMemory(Server server) {
		ServerInformation serverInfo = new ServerInformation(server, this.plugin, this.service, "memory");
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "elasticsearch_status.sh " + server.ip);
		String[] titles = { "heap.current", "heap.max", "heap.percent"};

		try {
			CommandExecutor.execCmdOnServer(server, c);
			FetchedData data = c.dataList.get(0);
			Formatter.fillDataPieces(data, 3, titles);

			for (int i = 0; i < 3; i++) {
				serverInfo.addData(data.getDataPieceByTitle(titles[i]));
			}

			// https://www.elastic.co/guide/en/elasticsearch/guide/current/_monitoring_individual_nodes.html
			Integer heapPercent = Integer.parseInt(data.getDataPieceByTitle("heap.percent").data);
			if (heapPercent > 75) {
				if (heapPercent > 85) {
					serverInfo.setStatus(Status.KO);
					serverInfo.addMessage("Danger, the heap is filled at more than 85%");
				} else {
					serverInfo.setStatus(Status.WARNING);
					serverInfo.addMessage("Warning, the heap is filled at more than 75%");
				}
			} else {
				serverInfo.setStatus(Status.OK);
				serverInfo.addMessage("The heap usage is OK (" + heapPercent + "%)");
			}
		} catch (Exception e) {
			serverInfo.setStatus(Status.WARNING);
			serverInfo.addMessage("Unable to fetch memory for Elasticsearch");
			logger.error("Unable to get memory for {}", service, e);

		}

		return serverInfo;
	}
	
	private ServerInformation checkFileDesc(Server server) {
		ServerInformation serverInfo = new ServerInformation(server, this.plugin, this.service, "filedesc");
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "elasticsearch_status.sh " + server.ip);
		String[] titles = {"heap.current", "heap.max", "heap.percent", "file_desc.current", "file_desc.max",
				"file_desc.percent" };

		try {
			CommandExecutor.execCmdOnServer(server, c);
			FetchedData data = c.dataList.get(0);
			
			Formatter.fillDataPieces(data, 6, titles);

			// first 3 results are memory results so skip them
			data.dataPieces = data.dataPieces.subList(3, 6);
 
			for (int i = 3; i < 6; i++) {
				serverInfo.addData(data.getDataPieceByTitle(titles[i]));
			}

			Integer fileDescPercent = Integer.parseInt(data.getDataPieceByTitle("file_desc.percent").data);
			if (fileDescPercent > 75) {
				if (fileDescPercent > 85) {
					serverInfo.setStatus(Status.KO);
					serverInfo.addMessage("Danger, there are too many file descriptors");
				} else {
					serverInfo.setStatus(Status.WARNING);
					serverInfo.addMessage("Warning, you are about to have too many file descriptors");
				}
			} else {
				serverInfo.setStatus(Status.OK);
				serverInfo.addMessage("There is a correct amount of file descriptors");
			}
		} catch (Exception e) {
			serverInfo.setStatus(Status.WARNING);
			serverInfo.addMessage("Unable to fetch file descriptor for Elasticsearch");
			logger.warn("Unable to fetch file descriptor for {}", service, e);
		}

		return serverInfo;
	}
}

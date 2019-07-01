package net.bluemind.monitoring.handler.services;


import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.server.api.Server;
import net.bluemind.monitoring.service.util.Formatter;


public abstract class AbstractJavaService extends AbstractService {

	public AbstractJavaService(String service, List<String> tags) {
		super(service, tags);
		
		this.endpoints.add("memory");
	}
	
	public AbstractJavaService(String service, String tag) {
		this(service, ImmutableList.of(tag));
	}
	
	public AbstractJavaService(String service) {
		this(service, (List<String>)null);
	}
	
	public ServerInformation memory(Server server) {
		ServerInformation bmService = new ServerInformation(server, ServicesHandler.BASE, this.service, "memory");
		Command command;

		try {
			command = checkMemory(server);
			bmService.commands.add(command);
			if (command.hasDataList() && command.dataList.get(0).data.indexOf('(') != -1) {
				String[] data = command.dataList.get(0).data.split("\\(");
				double memoryUsed = Formatter.humanReadabletoBytes(data[0]);
				double memoryTotal = Formatter.humanReadabletoBytes(StringUtils.chop(data[1]));
				bmService.addData(new FetchedData("memory used", memoryUsed + "MB"));
				bmService.addData(new FetchedData("memory total", memoryTotal + "MB"));
	
				int percent = (int) (memoryUsed / memoryTotal * 100) ;
				if (percent > 90) {
					bmService.setStatus(Status.WARNING);
					bmService.addMessage(bmService.service + " memory usage is too high");
				}
				bmService.setStatus(Status.OK);
				bmService.addMessage(bmService.service + " memory usage is OK (" + percent + "%)");
			} else {
				bmService.setStatus(Status.WARNING);
				bmService.addMessage("No used memory data for " + this.service);
			}
		} catch (Exception e) {
			bmService.setStatus(Status.WARNING);
			bmService.addMessage("Unable to get used memory for " + this.service);
			logger.warn("Error retrieving memory for " + this.service, e);
		}

		return bmService;

	}
	
	private Command checkMemory(Server server) throws ServerFault {

		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "used_memory.sh " + this.service);

		CommandExecutor.execCmdOnServer(server, c);

		return c;

	}

	@Override
	public ServerInformation getServerInfo(Server server, String method) {
		
		switch (method) {
		case "connection":
			return checkConnection(server);
		case "running":
			return checkRunning(server);
		case "memory":
			return memory(server);
		}
		
		return getSpecificServerInfo(server, method);
	}
	
	protected abstract ServerInformation getSpecificServerInfo(Server server, String method);


}

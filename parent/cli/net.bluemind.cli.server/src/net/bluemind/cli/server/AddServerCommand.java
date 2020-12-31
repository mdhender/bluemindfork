/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.server;

import java.util.Arrays;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


@Command(name = "add", description = "add new Bluemind server")
public class AddServerCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("server");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return AddServerCommand.class;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Option(names = { "--ip-address", "-i" }, required = true, description = "ip address value")
	public String serverIp = null;

	@Option(names = { "--fqdn", "-f" }, required = true, description = "FQDN value")
	public String serverFqdn = null;

	@Option(names = { "--name", "-n" }, required = true, description = "Name value")
	public String serverName = null;

	@Option(names = { "--tag", "-t" }, description = "Tag value.")
	public String[] tags = null;

	
	@Override
	public void run() {
		IServer serverService = ctx.adminApi().instance(IServer.class, "default");
		 
		Server h = new Server();
		h.ip = serverIp;
		h.name = serverName;
		h.fqdn = serverFqdn;
		h.tags = Arrays.asList(tags);
		
		TaskRef taskRef = serverService.create(serverName, h);
		TaskStatus ts = Tasks.follow(ctx, taskRef, "Could not create server.");
		
		if (ts.state == TaskStatus.State.Success) {
			ctx.info(String.format("Server %s created and tagged as %s", h.name, String.join(",", Arrays.asList(tags))));
		} else if (ts.state == TaskStatus.State.InError) {
			ctx.error(String.format("Can't create Server %s and tagged as %s", h.name, String.join(",", Arrays.asList(tags))));
		}
	}
 
}
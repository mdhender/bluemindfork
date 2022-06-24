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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "untags", description = "untag a server")
public class UnTagServerCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("server");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UnTagServerCommand.class;
		}
	}

	protected CliContext ctx;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Option(names = { "--server", "-s" }, required = true, description = "Server name")
	public String serverName = null;

	@Option(names = { "--tags", "-t" }, description = "Tags value to untag.")
	public String[] tags = null;

	@Override
	public void run() {
		IServer serverService = ctx.adminApi().instance(IServer.class, "default");
		ItemValue<Server> h = serverService.getComplete(serverName);

		List<String> newTags = h.value.tags.stream().filter(v -> !Arrays.asList(tags).contains(v))
				.collect(Collectors.toList());

		TaskRef taskRef = serverService.setTags(serverName, newTags);
		TaskStatus ts = Tasks.follow(ctx, taskRef, "", "Could not untag server.");

		if (ts.state == TaskStatus.State.Success) {
			ctx.info(String.format("Server %s is untagged as %s", serverName, String.join(",", Arrays.asList(tags))));
		} else if (ts.state == TaskStatus.State.InError) {
			ctx.error(String.format("Server %s cannot be untagged as %s", serverName,
					String.join(",", Arrays.asList(tags))));
		}
	}
}
/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.launcher;

import java.util.List;

import org.osgi.framework.Version;

import com.google.common.collect.ArrayListMultimap;

import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import io.airlift.airline.ParseArgumentsUnexpectedException;
import io.airlift.airline.ParseCommandUnrecognizedException;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.CmdLets;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;

public class CLIManager {
	private final Cli<ICmdLet> airliftCli;

	public CLIManager(Version v) {
		CliBuilder<ICmdLet> builder = Cli.builder("bm-cli");
		builder.withDescription("BlueMind CLI " + v.toString());

		List<ICmdLetRegistration> registrations = CmdLets.commands();
		ArrayListMultimap<String, Class<? extends ICmdLet>> commandByGroup = ArrayListMultimap.create();
		registrations.stream().forEach(reg -> {
			commandByGroup.put(reg.group().orElse("ROOT_COMMANDS"), reg.commandClass());
		});
		List<Class<? extends ICmdLet>> rootCommands = commandByGroup.get("ROOT_COMMANDS");
		rootCommands.forEach(klass -> builder.withCommand(klass));
		builder.withDefaultCommand(rootCommands.get(0));

		commandByGroup.keySet().stream().filter(group -> !"ROOT_COMMANDS".equals(group)).forEach(group -> {
			List<Class<? extends ICmdLet>> commands = commandByGroup.get(group);
			builder.withGroup(group).withDescription(group + " task(s)").withCommands(commands)
					.withDefaultCommand(commands.get(0));
		});

		airliftCli = builder.build();
	}

	public void processArgs(String... args) {
		CliContext ctx = CliContext.get();

		try {
			ICmdLet parsed = airliftCli.parse(args);
			Runnable toRun = parsed.forContext(ctx);
			toRun.run();
		} catch (ParseArgumentsUnexpectedException | ParseCommandUnrecognizedException e) {
			ctx.error(String.format("Invalid input: %s", e.getMessage()));
			airliftCli.parse(new String[] {}).forContext(null).run();
			throw e;
		} catch (CliException c) {
		} catch (Exception e) {
			ctx.error(e.getMessage());
			throw e;
		}
	}

}

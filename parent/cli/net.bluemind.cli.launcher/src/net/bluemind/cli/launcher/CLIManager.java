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
import java.util.stream.Collectors;

import org.osgi.framework.Version;

import com.google.common.collect.ArrayListMultimap;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.CmdLets;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.RunLast;
import picocli.CommandLine.Spec;

public class CLIManager {
	private final CommandLine mainCommand;

	@Command(mixinStandardHelpOptions = true, subcommands = { HelpCommand.class,
			GenerateCompletion.class }, exitCodeOnInvalidInput = 51, exitCodeOnExecutionException = 50, exitCodeListHeading = "Exit Codes:%n", exitCodeList = {
					" 0:Successful program execution",
					" 1:Internal software error: an exception occurred when invoking "
							+ "the business logic of this command.",
					"51:Usage error: user input for the command was incorrect, "
							+ "e.g., the wrong number of arguments, a bad flag, "
							+ "a bad syntax in a parameter, etc.", }, sortOptions = true)
	static class ParentCommand implements Runnable {
		@Spec
		CommandSpec spec;

		@Override
		public void run() {
			spec.commandLine().usage(System.err); // NOSONAR
		}
	}

	static class PrintExceptionMessageHandler implements IExecutionExceptionHandler {
		public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
			if (ex.getMessage() != null) {
				cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
			}
			if (!(ex instanceof CliException)) {
				ex.printStackTrace(cmd.getErr());
			}
			return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
					: cmd.getCommandSpec().exitCodeOnExecutionException();
		}
	}

	public CLIManager(Version v) {
		CliContext ctx = CliContext.get();

		mainCommand = new CommandLine(new ParentCommand());
		CommandSpec spec = mainCommand.getCommandSpec();
		spec.version(v.toString());
		spec.name("bm-cli");

		mainCommand.setExecutionExceptionHandler(new PrintExceptionMessageHandler());
		mainCommand.setCommandName("bm-cli");
		mainCommand.setExecutionStrategy(new RunLast());
		mainCommand.setAbbreviatedOptionsAllowed(true);
		mainCommand.setAbbreviatedSubcommandsAllowed(true);

		// Hide generate-completion from the help list
		CommandLine gen = mainCommand.getSubcommands().get("generate-completion");
		gen.getCommandSpec().usageMessage().hidden(true);

		List<ICmdLetRegistration> registrations = CmdLets.commands();
		ArrayListMultimap<String, Class<? extends ICmdLet>> commandByGroup = ArrayListMultimap.create();
		registrations.stream().forEach(reg -> {
			commandByGroup.put(reg.group().orElse("ROOT_COMMANDS"), reg.commandClass());
		});
		List<Class<? extends ICmdLet>> rootCommands = commandByGroup.get("ROOT_COMMANDS");

		commandByGroup.keySet().stream().sorted().filter(group -> !"ROOT_COMMANDS".equals(group)).forEach(group -> {
			List<Class<? extends ICmdLet>> commands = commandByGroup.get(group).stream()
					.sorted((c1, c2) -> c1.getName().compareTo(c2.getName())).collect(Collectors.toList());
			ParentCommand parentCmd = new ParentCommand();
			CommandLine groupCommand = new CommandLine(parentCmd);
			groupCommand.setCommandName(group);
			for (Class<? extends ICmdLet> cmd : commands) {
				try {
					groupCommand.addSubcommand(cmd.getDeclaredConstructor().newInstance().forContext(ctx));
				} catch (Exception e) {
					System.err.println("Unable to register subcommand: " + e); // NOSONAR
				}
			}
			mainCommand.addSubcommand(group, groupCommand);
		});

		rootCommands.forEach(cmd -> {
			try {
				mainCommand.addSubcommand(cmd.getDeclaredConstructor().newInstance().forContext(ctx));
			} catch (Exception e) {
				System.err.println("Unable to register subcommand: " + e); // NOSONAR
			}
		});

	}

	public int processArgs(String... args) {
		return mainCommand.execute(args);
	}

}

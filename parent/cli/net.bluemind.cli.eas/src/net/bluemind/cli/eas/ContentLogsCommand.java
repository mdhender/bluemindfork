/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.eas;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.eas.ContentLog.ContentLogForAll;
import net.bluemind.cli.eas.ContentLog.ContentLogForUser;
import net.bluemind.cli.utils.CliUtils;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "content-logs", header = { "Activate or Deactivate content logs for EAS users",
		"- by creating or deleting data.in.logs file", "- by adding or removing user entry to the file",
		"Empty data.in.logs file, activate content logs for all EAS users" })
public class ContentLogsCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("eas");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ContentLogsCommand.class;
		}
	}

	private static class Action {
		@Option(required = true, names = { "--enable" }, description = "Enable EAS content logs")
		Boolean enabled;
		@Option(required = true, names = { "--disable" }, description = "Disable EAS content logs")
		Boolean disabled;
	}

	private static class SetLogs {
		@ArgGroup(exclusive = false, multiplicity = "1")
		ContentLogForUser forUser;
		@ArgGroup(exclusive = false, multiplicity = "1")
		ContentLogForAll forAll;
	}

	@ArgGroup(exclusive = false, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@ArgGroup(exclusive = true, multiplicity = "1", heading = "Activate or deactivate EAS content logs%n")
		Action action;
		@ArgGroup(exclusive = true, multiplicity = "1", heading = "Set EAS content logs%n")
		SetLogs setLogs;
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public void run() {
		Optional.ofNullable(scope.action.enabled) //
				.ifPresentOrElse( //
						s -> Optional.ofNullable(scope.setLogs.forUser) //
								.ifPresentOrElse(a -> a.activate(ctx, cliUtils), //
										() -> Optional.ofNullable(scope.setLogs.forAll) //
												.ifPresentOrElse(a -> a.activate(ctx), //
														() -> new CliException("Activation cannot be done"))),
						() -> Optional.ofNullable(scope.action.disabled) //
								.ifPresentOrElse( //
										s -> Optional.ofNullable(scope.setLogs.forUser) //
												.ifPresentOrElse(a -> a.deactivate(ctx, cliUtils), //
														() -> Optional.ofNullable(scope.setLogs.forAll) //
																.ifPresentOrElse(a -> a.deactivate(ctx), //
																		() -> new CliException(
																				"Deactivation cannot be done"))),
										() -> new CliException("EAS content logs not updated")));
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}

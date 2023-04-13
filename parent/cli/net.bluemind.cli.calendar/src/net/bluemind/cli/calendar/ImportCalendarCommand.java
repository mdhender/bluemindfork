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
package net.bluemind.cli.calendar;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliTaskMonitor;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "import", description = "import an ICS File")
public class ImportCalendarCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("calendar");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ImportCalendarCommand.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = "--ics-file-path", description = "The path of the ics file. ex: </tmp/my_calendar.ics>")
	public Path icsFilePath;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@Option(names = "--calendarUid", required = false, description = "Import ICS to calendar UID")
		private String calendarUid;

		@Option(names = "--email", required = false, description = "Import ICS to default user calendar")
		private String email;
	}

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		File file = icsFilePath.toFile();
		if (!file.exists() || file.isDirectory()) {
			throw new CliException("File " + icsFilePath + " doesn't exist.");
		}

		Optional.ofNullable(scope.email).ifPresentOrElse(this::importByEmail,
				() -> runImportProcess(scope.calendarUid));
	}

	private void importByEmail(String email) {
		if (!Regex.EMAIL.validate(email)) {
			throw new CliException("Invalid email: " + email);
		}

		String userUid = Optional.ofNullable(cliUtils.getUserUidByEmail(email))
				.orElseThrow(() -> new CliException("User with email: " + email + " not found"));

		runImportProcess(Optional.ofNullable(ICalendarUids.defaultUserCalendar(userUid))
				.orElseThrow(() -> new CliException("No default calendar UID found for user: " + email)));
	}

	private void runImportProcess(String calendarUid) {
		if (dry) {
			ctx.info("DRY : calendar " + calendarUid + (scope.email != null ? " of " + scope.email : "")
					+ " was imported");
			return;
		}

		CliTaskMonitor rootMonitor = new CliTaskMonitor("import Calendar");
		try {
			rootMonitor.begin(2, "Begin import...");
			TaskRef taskRef = ctx.adminApi().instance(IVEvent.class, calendarUid)
					.importIcs(cliUtils.getStreamFromFile(icsFilePath));
			rootMonitor.progress(1, "Processing...");
			ITask task = ctx.adminApi().instance(ITask.class, taskRef.id);
			while (!task.status().state.ended) {
				switch (task.status().state) {
				case NotStarted:
					ctx.info("Not Started...");
					Thread.sleep(3000);
					break;
				case InProgress:
					ctx.info("Processing...");
					Thread.sleep(3000);
					break;
				case Success:
					rootMonitor.end(true, "Completed", "calendar " + calendarUid
							+ (scope.email != null ? " of " + scope.email : "") + " was imported");
					break;
				case InError:
					rootMonitor.end(false, "Failed", "calendar " + calendarUid + "import"
							+ (scope.email != null ? " of " + scope.email : "") + " was in error.");
					break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + task.status().state);
				}
			}
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}
}
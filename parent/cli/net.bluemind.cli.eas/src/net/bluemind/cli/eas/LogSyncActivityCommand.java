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

import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.eas.logparsing.ILogHandler;
import net.bluemind.cli.eas.logparsing.LogParser;
import net.bluemind.cli.eas.logparsing.SyncLogHandler;
import net.bluemind.cli.eas.logparsing.SyncLogHandler.DATA;
import net.bluemind.cli.eas.logparsing.SyncLogHandler.FilterOptions;
import net.bluemind.cli.eas.logparsing.SyncLogHandler.OutputOptions;
import net.bluemind.cli.eas.logparsing.SyncLogHandler.RESOLVE;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "sync_activity", header = { "List EAS logfile sync activity" })
public class LogSyncActivityCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("eas");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return LogSyncActivityCommand.class;
		}
	}

	@Option(required = false, names = { "--data" }, description = "dump wbxml data")
	boolean xml;

	@Option(required = false, defaultValue = "true", names = {
			"--resolve" }, description = "Resolve collection names on server")
	boolean resolve = true;

	@Option(required = false, names = { "--typefilter" }, description = "filter by type (EMAIL | CONTACT | CALENDAR)")
	String typeFilter;

	@Option(required = false, names = { "--collectionfilter" }, description = "filter by collectionId")
	String collectionId;

	@Parameters(paramLabel = "<file>", description = "EAS detail log file")
	public String file;

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public void run() {

		RESOLVE resolveOutput = resolve ? RESOLVE.LOOKUP : RESOLVE.NO;
		DATA dumpData = xml ? DATA.INCLUDE : DATA.EXCLUDE;

		IContainersFlatHierarchy service = null;
		if (resolve) {
			UserInfo user = getUserInfo(file);
			service = ctx.adminApi().instance(IContainersFlatHierarchy.class, user.domain(), user.userUid());
		}

		ILogHandler handler = new SyncLogHandler(service,
				new OutputOptions(resolveOutput, dumpData, new FilterOptions(collectionId, typeFilter)));

		try {
			String content = Files.readString(new File(file).toPath());
			new LogParser(content, handler).parse();
			ctx.info(handler.toTable());
		} catch (Exception e) {
			ctx.error("Cannot parse log file: {}", e.getMessage());
		}

	}

	private UserInfo getUserInfo(String name) {
		String simpleName = new File(name).getName();
		String stripBegin = simpleName.substring("user-eas-".length());
		String stripEnd = stripBegin.substring(0, stripBegin.indexOf(".log"));
		String[] split = stripEnd.split("_at_");
		String userUid = cliUtils.getUserUidByEmail(split[0] + "@" + split[1]);
		return new UserInfo(userUid, split[1]);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	public record UserInfo(String userUid, String domain) {

	}

}

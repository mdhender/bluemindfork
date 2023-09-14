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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.cli.auditlog;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.api.ILogRequestService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "fetch", description = "get auditlog data")
public class FetchAuditLogCommand implements Runnable, ICmdLet {

	private CliContext ctx;

	// TODO SLC : fournir une liste des principaux logtype à requêter
	@Option(names = "--logtype", required = true, description = "the logtype to query : calendar, mailbox_records, ...")
	public String logType;

	private CliUtils cliUtils;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("auditlog");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return FetchAuditLogCommand.class;
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		ILogRequestService service = ctx.adminApi().instance(ILogRequestService.class);
		AuditLogQuery auditLogQuery = new AuditLogQuery();
		auditLogQuery.logtype = logType;

//		service.queryMailLog(auditLogQuery)
	}
}

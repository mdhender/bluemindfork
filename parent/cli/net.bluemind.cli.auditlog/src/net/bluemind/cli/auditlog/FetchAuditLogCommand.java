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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.auditlogs.AuditLogEntry;
import net.bluemind.core.auditlogs.AuditLogQuery;
import net.bluemind.core.auditlogs.api.ILogRequestService;
import net.bluemind.core.utils.JsonUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "fetch", description = "get auditlog data")
public class FetchAuditLogCommand implements Runnable, ICmdLet {

	enum OutputFormat {
		json, table
	}

	enum enumLogType {
		calendar, mailbox_records, login, addressbook, dir
	}

	private static final String SEPARATOR = ",";
	private CliContext ctx;

	// TODO SCL : fournir une liste des principaux logtype à requêter
	@Option(names = "--logtype", required = true, description = "comma-separated logtypes to query : calendar, mailbox_records, ...")
	public String logTypes;

	@Option(names = "--with", required = false, description = "user mail address that must be present in audit logs")
	public String with;

	@Option(names = "--description", required = false, description = "event description that must be present in audit log")
	public String description;

	@Option(names = "--output", description = "output format to use (default ${DEFAULT-VALUE}): ${COMPLETION-CANDIDATES}")
	public OutputFormat outputFormat = OutputFormat.json;

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
		return this;
	}

	@Override
	public void run() {
		ILogRequestService service = ctx.adminApi().instance(ILogRequestService.class);
		String[] logtypesTable = logTypes.split(SEPARATOR);
		List<String> logTypesList = Arrays.asList(logtypesTable);
		for (String logType : logTypesList) {
			AuditLogQuery auditLogQuery = new AuditLogQuery();

			auditLogQuery.logtype = logType;
			if (with != null && !with.isBlank()) {
				auditLogQuery.with = with;
			}

			if (description != null && !description.isBlank()) {
				auditLogQuery.description = description;
			}

			List<AuditLogEntry> logMailQuery = service.queryAuditLog(auditLogQuery);

			if (outputFormat == OutputFormat.table) {
				String[] headers = { "timestamp", "logtype", "action", "container.owner.email", "content.description",
						"content.with", "content.key", "content.value", "updatemessage" };
				String[][] data = new String[logMailQuery.size()][headers.length];
				int index = 0;
				for (AuditLogEntry log : logMailQuery) {
					data[index][0] = log.timestamp.toString();
					data[index][1] = log.logtype;
					data[index][2] = log.action;
					data[index][3] = (log.container != null) ? log.container.ownerElement().email() : "";
					data[index][4] = (log.content != null) ? log.content.description() : "";
					data[index][5] = (log.content != null) ? log.content.with().toString() : "";
					data[index][6] = (log.content != null) ? log.content.key() : "";
					data[index][7] = (log.content != null) ? log.content.newValue() : "";
					data[index][8] = log.updatemessage;
					index++;
				}
				ctx.info(AsciiTable.getTable(headers, data));
			} else {
				logMailQuery.forEach(e -> ctx.info(JsonUtils.asString(e)));
			}
		}
	}
}

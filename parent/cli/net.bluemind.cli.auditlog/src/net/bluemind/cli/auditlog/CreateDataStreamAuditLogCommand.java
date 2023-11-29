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

package net.bluemind.cli.auditlog;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.auditlogs.client.es.datastreams.DataStreamActivator;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "create audit log datastream")
public class CreateDataStreamAuditLogCommand implements Runnable, ICmdLet {

	private static final Logger logger = LoggerFactory.getLogger(CreateDataStreamAuditLogCommand.class);

	static class DataStreamField {
		@Option(names = "--name", required = true, description = "datastream full name")
		String name;
		@Option(names = "--pattern", required = true, description = "datastream pattern (e.g. audit_log_%s, where %s will be replaced with domainUid)")
		String pattern;
	}

	@ArgGroup(exclusive = true, multiplicity = "1")
	public DataStreamField dataStreamField;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("auditlog");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CreateDataStreamAuditLogCommand.class;
		}

	}

	private CliContext ctx;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Override
	public void run() {

		DataStreamActivator dataStreamActivator = new DataStreamActivator();

		if (dataStreamField.name != null) {
			try {
				dataStreamActivator.createDataStream(dataStreamField.name);
				ctx.info("Datastream '" + dataStreamField.name + "' successfully created");
			} catch (ElasticsearchException | IOException e) {
				logger.error("Datastream '{}' creation failed: {}", dataStreamField.name, e.getMessage());
				ctx.error("Datastream creation failed");
			}
		}

		// Get all domains and create datastream for each one
		if (dataStreamField.pattern != null) {
			String pattern = dataStreamField.pattern;
			if (!isCompliantPattern(pattern)) {
				ctx.error("Pattern '" + pattern + "' must have format 'my_pattern_%s'");
				return;
			}
			IDomains service = ctx.adminApi().instance(IDomains.class);
			List<ItemValue<Domain>> domains = service.all();
			domains.forEach(d -> {
				String dataStreamFullName = resolveDataStreamPattern(d.uid);
				try {
					dataStreamActivator.createDataStream(dataStreamFullName);
					ctx.info("Datastream '" + dataStreamFullName + "' successfully created");
				} catch (ElasticsearchException | IOException e) {
					logger.error("Datastream '{}' creation failed: {}", dataStreamFullName, e.getMessage());
					ctx.error("Datastream creation failed");
				}
			});
		}

	}

	private String resolveDataStreamPattern(String domainUid) {
		return String.format(dataStreamField.pattern, domainUid);
	}

	private boolean isCompliantPattern(String pattern) {
		if (!dataStreamField.pattern.contains("%s")) {
			return false;
		}
		return true;
	}

}

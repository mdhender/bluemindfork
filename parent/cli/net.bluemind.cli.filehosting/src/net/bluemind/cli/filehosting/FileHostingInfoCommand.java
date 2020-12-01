/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.cli.filehosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import net.bluemind.attachment.api.Configuration;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;
import picocli.CommandLine.Command;

@Command(name = "info", description = "Show Filehosting implementation details")
public class FileHostingInfoCommand implements ICmdLet, Runnable {

	private CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		IFileHosting filehosting = ctx.adminApi().instance(IFileHosting.class, "global.virt");
		FileHostingInfo info = filehosting.info();
		ctx.info("Filehosting present: {}", info.present);
		if (info.present) {
			ctx.info("Filehosting info:{}{}{}", "\r\n", info.info, "\r\n");

			Set<String> domainsUids = printServers();
			printSettings(domainsUids);
		}

	}

	private Set<String> printServers() {
		IServer serverService = ctx.adminApi().instance(IServer.class, "default");
		List<ItemValue<Server>> servers = serverService.allComplete().stream()
				.filter(s -> s.value.tags.contains(TagDescriptor.bm_filehosting.getTag())).collect(Collectors.toList());

		Set<String> domainsUids = new HashSet<>();

		List<TblRow> serverInfoAsTable = new ArrayList<>();
		ctx.info("Filehosting servers:");
		for (ItemValue<Server> server : servers) {
			String uid = server.uid;
			String address = server.value.address();
			String name = server.value.name;

			List<String> assignedDomains = serverService.getServerAssignments(server.uid).stream()
					.filter(sa -> sa.tag.equals(TagDescriptor.bm_filehosting.getTag())).map(sa -> sa.domainUid)
					.collect(Collectors.toList());

			domainsUids.addAll(assignedDomains);
			String domains = String.join(",", assignedDomains);

			serverInfoAsTable.add(new TblRow(uid, address, name, domains));
		}

		ctx.info(AsciiTable.getTable(serverInfoAsTable, Arrays.asList( //
				new Column().header("UID").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(1)), //
				new Column().header("Address").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(2)), //
				new Column().header("Name").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(3)), //
				new Column().header("Domains").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(4)))));
		return domainsUids;
	}

	private void printSettings(Set<String> domainsUids) {
		List<TblRow> settingsAsTable = new ArrayList<>();
		ctx.info("Domain settings:");

		IGlobalSettings settingsGlobal = ctx.adminApi().instance(IGlobalSettings.class);
		Configuration config = config(settingsGlobal.get());
		settingsAsTable.add(new TblRow("global.virt", "" + config.maxFilesize, "" + config.autoDetachmentLimit,
				"" + config.retentionTime));

		for (String domainUid : domainsUids) {
			IDomainSettings settingsDomain = ctx.adminApi().instance(IDomainSettings.class, domainUid);
			Map<String, String> valuesDomain = settingsDomain.get();
			config = config(valuesDomain);
			settingsAsTable.add(new TblRow(domainUid, "" + config.maxFilesize, "" + config.autoDetachmentLimit,
					"" + config.retentionTime));

		}

		ctx.info(AsciiTable.getTable(settingsAsTable, Arrays.asList( //
				new Column().header("Domain").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(1)), //
				new Column().header("Max filesize").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(2)), //
				new Column().header("Auto-detachment limit").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(3)), //
				new Column().header("Retention").dataAlign(HorizontalAlign.LEFT).with(r -> r.value(4)))));
	}

	private Configuration config(Map<String, String> values) {
		Configuration config = new Configuration();
		config.autoDetachmentLimit = longValue(values, GlobalSettingsKeys.mail_autoDetachmentLimit.name(), 0);
		config.maxFilesize = longValue(values, GlobalSettingsKeys.filehosting_max_filesize.name(), 0);
		config.retentionTime = longValue(values, GlobalSettingsKeys.filehosting_retention.name(), 365).intValue();
		return config;
	}

	private Long longValue(Map<String, String> map, String key, long defaultValue) {
		String value = map.get(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Long.valueOf(value);
		}
	}

	private static class TblRow {
		List<String> columnValues;

		public TblRow(String... values) {
			this.columnValues = new ArrayList<>();
			for (String value : values) {
				columnValues.add(value);
			}
		}

		public String value(int column) {
			return columnValues.get(column - 1);
		}
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("filehosting");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return FileHostingInfoCommand.class;
		}

	}

}

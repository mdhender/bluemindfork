/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.user.externalaccount;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.user.api.IUserExternalAccount;
import net.bluemind.user.api.UserAccount;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CSV format
 * 
 * externalSystemIdentifier;user@bluemind;user@externalsystem
 * 
 * ex: Teams;david@bm.lan;david@outlook.com
 *
 */
@Command(name = "externalaccountimport", description = "create or update external accounts")
public class ExternalAccountImportCommand implements ICmdLet, Runnable {

	@Option(names = "--file", required = true, description = "External accounts CSV file")
	public Path file = null;

	@Option(names = "--domain", required = true, description = "the domain to import external accounts")
	public String domain;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	protected CliContext ctx;
	protected CliUtils cliUtils;

	private static String CSV_SEPARATOR = ";";
	private ItemValue<Domain> domainItem;

	private class ExternalAccount {
		private final String externalSystemIdentifier;
		private final String bmLogin;
		private final String externalSystemLogin;

		public ExternalAccount(String externalSystemIdentifier, String login, String externalSystemLogin) {
			this.externalSystemIdentifier = externalSystemIdentifier;
			this.bmLogin = login;
			this.externalSystemLogin = externalSystemLogin;
		}

		public String getExternalSystemIdentifier() {
			return externalSystemIdentifier;
		}

		public String getBmLogin() {
			return bmLogin;
		}

		public String getExternalAccountLogin() {
			return externalSystemLogin;
		}

		@Override
		public String toString() {
			return "ExternalAccount [externalSystemIdentifier=" + externalSystemIdentifier + ", bmLogin=" + bmLogin
					+ ", externalSystemLogin=" + externalSystemLogin + "]";
		}

	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ExternalAccountImportCommand.class;
		}
	}

	@Override
	public void run() {
		ctx.info("External accounts import...");

		checkParams();
		List<ExternalAccount> externalAccounts = parseCSVFile();
		externalAccounts.forEach(externalAccount -> {
			IDirectory directoryService = ctx.adminApi().instance(IDirectory.class, domainItem.uid);
			DirEntry dirEntry = directoryService.getByEmail(externalAccount.getBmLogin());
			if (dirEntry == null) {
				ctx.error("DirEntry {} not found", externalAccount.getBmLogin());
				return;
			}
			if (dirEntry.kind != Kind.USER) {
				ctx.error("Unsupported DirEntry kind {} / {}", externalAccount.getBmLogin(), dirEntry.kind);
				return;
			}

			IUserExternalAccount externalAccountService = ctx.adminApi().instance(IUserExternalAccount.class,
					domainItem.uid, dirEntry.entryUid);
			UserAccount userAccount = externalAccountService.get(externalAccount.getExternalSystemIdentifier());
			if (userAccount == null) {
				if (dry) {
					ctx.info("[dry] Should create external account {}", externalAccount.toString());
				} else {
					ctx.info("Creating external account {}", externalAccount.toString());
					externalAccountService.create(externalAccount.getExternalSystemIdentifier(),
							new UserAccount(externalAccount.getExternalAccountLogin()));
				}
			} else {
				if (dry) {
					ctx.info("[dry] Should update external account {}", externalAccount.toString());
				} else {
					ctx.info("Updating external account {}", externalAccount.toString());
					externalAccountService.update(externalAccount.getExternalSystemIdentifier(),
							new UserAccount(externalAccount.getExternalAccountLogin()));
				}
			}

		});

	}

	private void checkParams() {
		domainItem = cliUtils.getDomain(domain)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));

		if (!file.toFile().exists()) {
			throw new CliException(String.format("File %s not found", file.toAbsolutePath()));
		}
	}

	private List<ExternalAccount> parseCSVFile() {
		List<ExternalAccount> ret = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] values = line.split(CSV_SEPARATOR);
				ret.add(new ExternalAccount(values[0], values[1], values[2]));
			}
		} catch (Exception e) {
			new CliException(e);
		}
		return ret;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.auth.provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.ExternalSystem.AuthKind;
import net.bluemind.system.api.IExternalSystem;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;

@Command(name = "openid-list-provider", description = "List registered OpenId provider systems")
public class ListOauthSystems implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("auth-provider");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return ListOauthSystems.class;
		}
	}

	private CliContext ctx;

	@Override
	public void run() {
		IExternalSystem extSystemService = ctx.adminApi().instance(IExternalSystem.class);
		List<ExternalSystem> externalSystemsByAuthKind = extSystemService
				.getExternalSystemsByAuthKind(new HashSet<>(Arrays.asList(AuthKind.OPEN_ID_PKCE)));

		int size = externalSystemsByAuthKind.size();
		String[] headers = { "Identifier", "Description", "Type" };
		String[][] asTable = new String[size][headers.length];

		int i = 0;
		for (ExternalSystem entry : externalSystemsByAuthKind) {
			asTable[i][0] = entry.identifier;
			asTable[i][1] = entry.description;
			asTable[i][2] = entry.authKind.name();
			i++;
		}
		ctx.info(AsciiTable.getTable(headers, asTable));

	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}

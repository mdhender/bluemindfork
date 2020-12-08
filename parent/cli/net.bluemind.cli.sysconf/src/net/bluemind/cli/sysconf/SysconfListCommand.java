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
package net.bluemind.cli.sysconf;

import java.util.Map;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.Command;

@Command(name = "list", description = "Show Sysconf values.")
public class SysconfListCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SysconfListCommand.class;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	@Override
	public void run() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
		SystemConf sysConf = configurationApi.getValues();
		display(sysConf.values);
	}

	private void display(Map<String, String> map) {
		// Used to add a row to include the header
		int size = map.size() + 1;

		String[] headers = { "Attribute", "Value" };
		String[][] asTable = new String[size][headers.length];

		int i = 1;
		for (Map.Entry<String, String> entry : map.entrySet()) {
			asTable[i][0] = entry.getKey();
			asTable[i][1] = entry.getValue();
			i++;
		}
		ctx.info(AsciiTable.getTable(headers, asTable));
	}

}
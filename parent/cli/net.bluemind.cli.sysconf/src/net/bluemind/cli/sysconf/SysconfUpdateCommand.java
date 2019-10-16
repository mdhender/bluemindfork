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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.airlift.airline.Arguments;
import io.airlift.airline.Option;
import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.system.api.ISystemConfiguration;


@Command(name = "set", description = "Set a value by using a parameter")
public class SysconfUpdateCommand implements ICmdLet, Runnable {
	
	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SysconfUpdateCommand.class;
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
	
	@Arguments(title="key", required = true, description = "key")
	public String key = null;
	
	@Option(name="{--value, -v}",required = true, description = "value")
	public String value = null;

	
	@Override
	public void run() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);

		Map<String, String> map = new HashMap<>();
		map.put(key, value);
		if (!map.isEmpty()){
			configurationApi.updateMutableValues(map);
		}
	}



}
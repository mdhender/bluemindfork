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
package net.bluemind.cli.hz;

import java.util.List;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.hornetq.client.MQ;
import picocli.CommandLine.Command;

@Command(name = "list", description = "Print a list of active cluster topics")
public class HzListCommand extends AbstractHzOperation {

	public static class Reg extends HzReg {

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return HzListCommand.class;
		}

	}

	@Override
	protected void connectedOperation() {
		List<String> objects = MQ.topics();
		for (String s : objects) {
			ctx.info(s);
		}
	}

}

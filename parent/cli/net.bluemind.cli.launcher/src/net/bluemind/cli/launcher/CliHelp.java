/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.launcher;

import java.util.Optional;

import io.airlift.airline.Help;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;

/**
 * The automated help command from airlift
 *
 */
public class CliHelp extends Help implements ICmdLet {

	public static final class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.empty();
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CliHelp.class;
		}

	}

	public CliHelp() {
		super();
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		return this;
	}

}

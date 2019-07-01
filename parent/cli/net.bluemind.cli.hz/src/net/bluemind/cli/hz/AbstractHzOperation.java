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
package net.bluemind.cli.hz;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.hornetq.client.MQ;

public abstract class AbstractHzOperation implements ICmdLet, Runnable {

	public static abstract class HzReg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("hz");
		}

	}

	@Override
	public final Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	protected CliContext ctx;

	@Override
	public final void run() {
		try {
			MQ.init().get(10, TimeUnit.SECONDS);
			connectedOperation();
		} catch (Exception e) {
			throw new CliException(e);
		}

	}

	protected abstract void connectedOperation();

}

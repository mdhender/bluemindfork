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
package net.bluemind.cli.node;

import java.util.Optional;

import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import picocli.CommandLine.Command;

/**
 * This is the defaut command on node related stuff to ensure we don't run a
 * destructive op by default
 *
 */
@Command(name = "status", description = "Show node(s) availability")
public class StatusCommand extends AbstractNodeOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("node");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return StatusCommand.class;
		}

	}

	@Override
	protected void synchronousServerOperation(IServer serversApi, ItemValue<Server> srv) {
		try {
			byte[] content = serversApi.readFile(srv.uid, "/etc/bm/bm.ini");
			if (content != null && content.length > 0) {
				reportSuccess(srv);
			} else {
				reportFailure(srv);
			}
		} catch (ServerFault sf) {
			reportFailure(srv);
		}
	}

	private void reportFailure(ItemValue<Server> srv) {
		System.out.println(ctx.ansi().a(buildResult(srv)).fgBrightRed().a("FAILED").reset());
	}

	private void reportSuccess(ItemValue<Server> srv) {
		System.out.println(ctx.ansi().a(buildResult(srv)).fgBrightGreen().a("OK").reset());
	}

	private String buildResult(ItemValue<Server> srv) {
		return "Server " + srv.value.address() + " (" + srv.uid + " " + srv.displayName + ") ";
	}

}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.user;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "logging", description = "Enable/Disable per-user logs")
public class LoggingCommand extends SingleOrDomainOperation {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return LoggingCommand.class;
		}
	}

	@Option(names = "--enable", description = "Enable the per-user logs (disable if not specified)")
	public boolean enable = false;

	public enum Endpoint {
		MAPI, IMAP, POP3;
	}

	@Option(names = "--endpoint", description = "On which endpoint should we enable per-user logs (${COMPLETION-CANDIDATES})")
	public Endpoint endpoint;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		ctx.info("Switch logs {} for {} (user uid: {}) on {}", (enable ? "ON" : "OFF"), de.displayName, de.uid,
				endpoint);
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		userApi.enablePerUserLog(de.uid, endpoint.name().toLowerCase(), enable);
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}
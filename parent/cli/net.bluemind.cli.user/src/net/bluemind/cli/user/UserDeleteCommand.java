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
package net.bluemind.cli.user;

import java.util.Optional;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;

@Command(name = "delete", description = "delete user")
public class UserDeleteCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserDeleteCommand.class;
		}
	}

	@Option(name = "--dry", description = "Dry-run (do nothing)")
	public boolean dry = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		if (dry) {
			System.out.println("DRY : delete " + de.displayName);
		} else {
			IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
			TaskRef tr = userApi.delete(de.uid);
			TaskStatus status = Tasks.follow(ctx, tr, String.format("Fail to delete entry %s", de));

			if (status == null || status.state != TaskStatus.State.Success) {
				System.err.println("Failed to delete user " + de.displayName);
			} else {
				System.out.println("user " + de.displayName + " deleted");
			}
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}

}

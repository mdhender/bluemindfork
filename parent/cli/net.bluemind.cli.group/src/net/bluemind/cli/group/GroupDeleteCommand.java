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
package net.bluemind.cli.group;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "delete group")
public class GroupDeleteCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("group");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return GroupDeleteCommand.class;
		}
	}

	@Parameters(paramLabel = "<target>", description = "groupName@domain or domain name")
	public String target;

	@Option(names = "--dry", description = "Dry-run (do nothing)")
	public boolean dry;

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Override
	public void run() {
		if (target.contains("@")) {
			deleteGroupbyName();
		} else {
			deleteAllGroups();
		}
	}

	private void deleteGroupbyName() {
		String domainUid = target.split("@")[1];
		String name = target.split("@")[0];

		IGroup groupApi = ctx.adminApi().instance(IGroup.class, domainUid);
		ItemValue<Group> group = groupApi.byName(name);
		if (group != null) {
			deleteGroup(groupApi, group.uid);
		}

	}

	private void deleteAllGroups() {
		IGroup groupApi = ctx.adminApi().instance(IGroup.class, target);
		groupApi.allUids().forEach(uid -> deleteGroup(groupApi, uid));
	}

	private void deleteGroup(IGroup groupApi, String uid) {
		if (dry) {
			ctx.info("DRY : delete " + uid);
		} else {
			TaskRef tr = groupApi.delete(uid);
			TaskStatus status = Tasks.follow(ctx, tr, String.format("Failed to delete entry %s", uid));

			if (status == null || status.state != TaskStatus.State.Success) {
				ctx.error("Failed to delete group " + uid);
			} else {
				ctx.info("Group " + uid + " deleted");
			}
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}

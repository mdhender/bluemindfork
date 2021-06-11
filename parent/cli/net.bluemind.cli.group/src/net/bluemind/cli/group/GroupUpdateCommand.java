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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "update", description = "update users")
public class GroupUpdateCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("group");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return GroupUpdateCommand.class;
		}
	}

	@Parameters(paramLabel = "<target>", description = "groupName@domain")
	public String target;

	@Option(names = "--add-members", description = "Add Members, use uid separated by spaces")
	public String appendMembers;

	@Option(names = "--remove-members", description = "Remove Members, use 'all' to empty group"
			+ "OR uid separated by spaces.")
	public String deleteMembers;

	@Option(names = "--extId", description = "update group externalId")
	public String extId;

	@Option(names = "--description", description = "update group description")
	public String description;

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
		if (!target.contains("@")) {
			ctx.info("Group not found.");
			return;
		}

		cliUtils.getDomain(target).ifPresent(domain -> {
			String name = target.split("@")[0];

			IGroup groupApi = ctx.adminApi().instance(IGroup.class, domain.uid);
			ItemValue<Group> group = groupApi.byName(name);

			if (extId != null) {
				groupApi.setExtId(group.uid, extId.trim().isEmpty() ? null : extId);
			}
			if (description != null) {
				group.value.description = description;
				groupApi.update(group.uid, group.value);
			}
			if (deleteMembers != null) {
				removeMembers(groupApi, group, domain.uid);
			}
			if (appendMembers != null) {
				addMembers(groupApi, group, domain.uid);
			}
		});
	}

	private void addMembers(IGroup groupApi, ItemValue<Group> group, String domainUid) {
		List<String> uids = Arrays.asList(appendMembers.split(" "));
		List<Member> allMembers = new ArrayList<>();

		allMembers.addAll(getMembersList(uids, domainUid));
		groupApi.add(group.uid, allMembers);
	}

	private void removeMembers(IGroup groupApi, ItemValue<Group> group, String domainUid) {
		if (deleteMembers.equalsIgnoreCase("all")) {
			groupApi.remove(group.uid, groupApi.getMembers(group.uid));
		} else {
			List<String> uids = Arrays.asList(deleteMembers.split(" "));
			groupApi.remove(group.uid, getMembersList(uids, domainUid));
		}
	}

	private List<Member> getMembersList(List<String> entryUids, String domainUid) {
		List<Member> memberList = new ArrayList<>();
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);
		DirEntryQuery dirQuery = new DirEntryQuery();
		dirQuery.entryUidFilter = entryUids;
		ListResult<ItemValue<DirEntry>> entries = dirApi.search(dirQuery);
		for (ItemValue<DirEntry> entry : entries.values) {
			Member member = new Member();
			member.uid = entry.uid;
			if (entry.value.kind == Kind.USER) {
				member.type = Type.user;
			}
			if (entry.value.kind == Kind.GROUP) {
				member.type = Type.group;
			}
			if (entry.value.kind == Kind.EXTERNALUSER) {
				member.type = Type.external_user;
			}
			memberList.add(member);
		}
		return memberList;
	}

}

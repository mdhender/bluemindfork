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
import java.util.List;
import java.util.Optional;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.externaluser.api.IExternalUser;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import net.bluemind.user.api.IUser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "display groups")
public class GroupGetCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("group");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return GroupGetCommand.class;
		}
	}

	protected CliContext ctx;
	protected CliUtils cliUtils;

	@Parameters(paramLabel = "<target>", description = "GroupName@domain or domain name")
	public String target;

	@Option(names = "--show-members", description = "Show members and group properties")
	boolean showMembers;

	@Option(names = "--resolved-members", description = "show members by email")
	public boolean resolvedMembers;

	@Option(names = "--expand-members", description = "show members and members of sub-groups.")
	public boolean expandMembers;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

	private JsonArray membersJson = new JsonArray();

	@Override
	public void run() {
		if (target.contains("@")) {
			findAGroup();
		} else {
			findAllGroups();
		}
	}

	private void findAGroup() {
		String domainUid = target.split("@")[1];
		String name = target.split("@")[0];
		try {
			IGroup groupApi = ctx.adminApi().instance(IGroup.class, domainUid);
			ItemValue<Group> group = groupApi.byName(name);
			if (group != null) {
				displayAllGroups(groupApi, target, group.uid);
			}
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}

	private void findAllGroups() {
		try {
			IGroup groupApi = ctx.adminApi().instance(IGroup.class, target);
			groupApi.allUids().forEach(uid -> displayAllGroups(groupApi, target, uid));
		} catch (Exception e) {
			throw new CliException(e.getMessage());
		}
	}

	private void displayAllGroups(IGroup groupApi, String domainUid, String uid) {
		if (!showMembers) {
			ctx.info(JsonUtils.asString(groupApi.getComplete(uid)));
		} else {
			displayAll(groupApi, domainUid, uid);
		}
	}

	private void displayAll(IGroup groupApi, String domainUid, String uid) {
		ItemValue<Group> group = groupApi.getComplete(uid);
		JsonObject groupJson = new JsonObject();
		groupJson.put("uid", group.uid);
		groupJson.put("extid", group.externalId);
		groupJson.put("displayName", group.displayName);
		groupJson.put("name", group.value.name);
		groupJson.put("description", group.value.description);
		groupJson.put("memberCount", Integer.toString(group.value.memberCount));

		JsonArray emailsJson = new JsonArray();
		group.value.emails.forEach(e -> emailsJson.add(e.address));
		groupJson.put("emails", emailsJson);

		List<Member> members = new ArrayList<>();
		if (expandMembers) {
			members = groupApi.getExpandedMembers(group.uid);
		} else {
			members = groupApi.getMembers(group.uid);
		}
		membersJson = new JsonArray();
		members.forEach(m -> getMember(m, domainUid));
		groupJson.put("members", membersJson);

		ctx.info(groupJson.encode());
	}

	private void getMember(Member member, String domainUid) {
		JsonObject memberJson = new JsonObject();
		if (resolvedMembers) {
			memberJson.put(member.type.toString(), getEmailAddress(member, domainUid));
		} else {
			memberJson.put(member.type.toString(), member.uid);
		}
		membersJson.add(memberJson);
	}

	private String getEmailAddress(Member member, String domainUid) {
		if (member.type == Type.user) {
			IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
			return userApi.getComplete(member.uid).value.login.concat(domainUid);
		}
		if (member.type == Type.group) {
			IGroup groupApi = ctx.adminApi().instance(IGroup.class, domainUid);
			return groupApi.getComplete(member.uid).value.name.concat(domainUid);
		}
		if (member.type == Type.external_user) {
			IExternalUser extUserApi = ctx.adminApi().instance(IExternalUser.class, domainUid);
			return extUserApi.getComplete(member.uid).value.defaultEmailAddress();
		}
		return null;
	}

	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.GROUP };
	}

}

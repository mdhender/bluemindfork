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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Strings;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get", description = "display users")
public class UserGetCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserGetCommand.class;
		}
	}

	@Option(names = "--display", description = "attributes to display separated by spaces can be :"
			+ "email, uid, extId, quota, aliases, familyNames, givenNames")
	public String display = null;

	@Option(names = "--archived", description = "only get archived users")
	public boolean archived = false;

	@Option(names = "--hidden", description = "only get hidden users")
	public boolean hidden = false;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		// be default, we want hidden users but not archived ones
		if ((archived != de.value.archived) || (hidden && !de.value.hidden)) {
			return;
		}
		if (de.value.system) {
			return;
		}

		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		if (display == null) {
			System.out.println(JsonUtils.asString(userApi.getComplete(de.uid)));
		} else {
			Set<String> displaySet = new HashSet<String>();
			if (!Strings.isNullOrEmpty(display)) {
				displaySet.addAll(Arrays.asList(display.split(" ")));
			}

			List<String> itemValues = Arrays.asList("uid", "email", "extId");
			boolean fast = displaySet.stream().allMatch(v -> itemValues.contains(v));

			JsonObject userJson = new JsonObject();
			if (displaySet.contains("uid")) {
				userJson.put("uid", de.uid);
			}
			if (displaySet.contains("email")) {
				userJson.put("email", de.value.email);
			}
			if (displaySet.contains("extId")) {
				userJson.put("extId", de.externalId);
			}

			if (!fast) {
				ItemValue<User> user = userApi.getComplete(de.uid);
				if (displaySet.contains("login")) {
					userJson.put("login", user.value.login);
				}
				if (displaySet.contains("quota")) {
					userJson.put("quota", user.value.quota);
				}
				if (displaySet.contains("familyNames")) {
					userJson.put("familyNames", user.value.contactInfos.identification.name.familyNames);
				}
				if (displaySet.contains("givenNames")) {
					userJson.put("givenNames", user.value.contactInfos.identification.name.givenNames);
				}
				if (displaySet.contains("aliases")) {
					userJson.put("aliases", user.value.login);
					JsonArray aliasJson = new JsonArray();
					user.value.contactInfos.communications.emails.forEach(e -> aliasJson.add(e.value));
					userJson.put("aliases", aliasJson);
				}
			}
			ctx.info(userJson.encode());
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}

}

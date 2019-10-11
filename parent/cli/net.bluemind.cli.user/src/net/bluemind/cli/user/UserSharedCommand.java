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

import java.util.List;
import java.util.Optional;

import org.vertx.java.core.json.JsonObject;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUserSubscription;

@Command(name = "shared", description = "Show shared containers for a user")
public class UserSharedCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserSharedCommand.class;
		}
	}
	
	@Option(name = "--byUser", description = "Show Containers shared by the user")
	public Boolean byUser = true;
	
	@Option(name = "--byOtherUsers", description = "Show Shared Containers from other users")
	public Boolean byOtherUsers = false;
	
	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {	
		/*if (de.uid.equals("admin0@global.virt")) {
			// only allow password update fir admin0
			if (extId != null || quota != null) {
				throw new CliException("extId and quota modification aren't allowed for admin0");
			}
		}*/
		if (byUser) {
			getSharedContainerbyUser(domainUid, de);
		}
		if (byOtherUsers) {
			getSharedContainersbyOtherUsers(domainUid, de);
		}
		
	}
	
	private void getSharedContainerbyUser(String domainUid, ItemValue<DirEntry> de) {
		IContainers containersApi = ctx.adminApi().instance(IContainers.class, domainUid);
		ContainerQuery query = new ContainerQuery();
		query.owner = de.uid;
		List<ContainerDescriptor> containers = containersApi.all(query);
		
		for (ContainerDescriptor containerDescriptor : containers) {
			JsonObject userSubJson = new JsonObject();
			userSubJson.putString("type", containerDescriptor.type);
			userSubJson.putString("uid", containerDescriptor.uid);
			userSubJson.putString("name", containerDescriptor.name);
			userSubJson.putString("owner", containerDescriptor.owner);
			ctx.info(userSubJson.toString());
			IContainerManagement containerManager = ctx.adminApi().instance(IContainerManagement.class, containerDescriptor.uid);
			List<AccessControlEntry> acls = containerManager.getAccessControlList();
			JsonObject aclJson = new JsonObject();
			for (AccessControlEntry acl : acls) {
				aclJson.putString("Subect", acl.subject);
				aclJson.putString("verb", acl.verb.toString());		
			}
			ctx.info(aclJson.toString());
		}
	}

	private void getSharedContainersbyOtherUsers(String domainUid, ItemValue<DirEntry> de) {
		IUserSubscription userSubApi = ctx.adminApi().instance(IUserSubscription.class, domainUid);
		List<ContainerSubscriptionDescriptor> subscriptions = userSubApi.listSubscriptions(de.uid, "calendar");
		subscriptions.addAll(userSubApi.listSubscriptions(de.uid, "addressbook"));
		subscriptions.addAll(userSubApi.listSubscriptions(de.uid, "todolist"));
		
		JsonObject userSubJson = new JsonObject();
		for (ContainerSubscriptionDescriptor containerSubscriptionDescriptor : subscriptions) {
			userSubJson.putString("type", containerSubscriptionDescriptor.containerType);
			userSubJson.putString("uid", containerSubscriptionDescriptor.containerUid);
			userSubJson.putString("name", containerSubscriptionDescriptor.name);
			userSubJson.putString("owner", containerSubscriptionDescriptor.owner);
			userSubJson.putString("offlineSync", Boolean.toString(containerSubscriptionDescriptor.offlineSync));
			userSubJson.putString("ownerDisplayName", containerSubscriptionDescriptor.ownerDisplayName);
		}
		
		ctx.info(userSubJson.toString());
	}


	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}

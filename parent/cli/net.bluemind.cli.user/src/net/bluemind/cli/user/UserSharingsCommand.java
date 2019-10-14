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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.freva.asciitable.AsciiTable;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

@Command(name = "sharings", description = "Show shared containers for a user")
public class UserSharingsCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserSharingsCommand.class;
		}
	}
	
	@Option(name = "--given", description = "Show Containers shared by the user")
	public Boolean given = false;
	
	@Option(name = "--received", description = "Show Containers Shared by other users")
	public Boolean received = false;
	
	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		IContainers containersApi = ctx.adminApi().instance(IContainers.class, de.uid);
		ContainerQuery query = new ContainerQuery();
		query.owner = de.uid;
		List<ContainerDescriptor> containers = containersApi.allForUser(domainUid, de.uid, query);
		if (given) {
			getAclsAndContainers(containers, de, domainUid, true);
		}
		else if(received) {
			getAclsAndContainers(containers, de, domainUid, false);
		}
			
	}
	
	private void getAclsAndContainers(List<ContainerDescriptor> containers, ItemValue<DirEntry> de, String domainUid, Boolean owned) {
		Map<ContainerDescriptor, List<AccessControlEntry>> map = new HashMap<ContainerDescriptor, List<AccessControlEntry>>();
		int aclsNumbers = 0;
		
		for (ContainerDescriptor containerDescriptor : containers) {
			if(containerDescriptor.owner.equalsIgnoreCase(de.uid) == owned) {			
				IContainerManagement containerManager = ctx.adminApi().instance(IContainerManagement.class, containerDescriptor.uid);
				List<AccessControlEntry> acls = containerManager.getAccessControlList();
				List<AccessControlEntry> aclsFiltered = new ArrayList<>();
				for (AccessControlEntry acl : acls) {
					//Do not garbage your own shares
					if(acl.subject.equalsIgnoreCase(de.uid) != owned) {
						aclsFiltered.add(acl);
					}
				}
				aclsNumbers += aclsFiltered.size();
				if(!aclsFiltered.isEmpty()) {
					map.put(containerDescriptor, aclsFiltered);
				}
			}
		}
		display(map, aclsNumbers, domainUid);
	}
	private void display(Map<ContainerDescriptor, List<AccessControlEntry>> map, int size, String domainUid) {
		//Used to add a row to include the header
		size++;
		
		String[][] asTable = new String[size][8];
		asTable[0][0] = "Owner DisplayName";
		asTable[0][1] = "Owner";
		asTable[0][2] = "Container Name";
		asTable[0][3] = "Container Uid";
		asTable[0][4] = "Container Type";
		asTable[0][5] = "DisplayName";
		asTable[0][6] = "Subject";
		asTable[0][7] = "Verb";
		
		int i = 1;
		for (Map.Entry<ContainerDescriptor, List<AccessControlEntry>> entry : map.entrySet()) {
			ContainerDescriptor containerInfos = entry.getKey();
			List<AccessControlEntry> acls = entry.getValue();
			for (AccessControlEntry acl : acls) {
				asTable[i][0] = containerInfos.ownerDisplayname;
				asTable[i][1] = containerInfos.owner;
				asTable[i][2] = containerInfos.name;
				asTable[i][3] = containerInfos.uid;
				asTable[i][4] = containerInfos.type;
				asTable[i][5] = resolvedSubject(acl.subject, domainUid);
				asTable[i][6] = acl.subject;
				asTable[i][7] = acl.verb.toString();
				i++;
			}
		}
		ctx.info(AsciiTable.getTable(asTable));
	}
	
	
	private String resolvedSubject(String subject, String domainUid) {
		if(subject.equalsIgnoreCase(domainUid)) {
			return domainUid;
		}
		IUser userApi = ctx.adminApi().instance(IUser.class, domainUid);
		ItemValue<User> user = userApi.getComplete(subject);
		if(user != null) {
			return user.displayName;
		} else {
			return "NO USER FOUND";
		}
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}

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
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

@Command(name = "sharings", description = "Show containers shared for a user")
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
	
	@Option(name = "--given", description = "Show containers shared by the user")
	public Boolean given = false;
	
	@Option(name = "--received", description = "Show containers Shared by other users")
	public Boolean received = false;
	
	@Option(name = "--expand", description = "Expand groups when a Container is shared to a group")
	public Boolean expand = false;
	
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
		Map<ContainerDescriptor, Map<DirEntry, String>> map = new HashMap<ContainerDescriptor, Map<DirEntry,String>>();
		Map<DirEntry, String> userInfos = new HashMap<DirEntry, String>();
		int aclsNumbers = 0;
		
		for (ContainerDescriptor containerDescriptor : containers) {
			if(containerDescriptor.owner.equalsIgnoreCase(de.uid) == owned) {			
				IContainerManagement containerManager = ctx.adminApi().instance(IContainerManagement.class, containerDescriptor.uid);
				List<AccessControlEntry> acls = containerManager.getAccessControlList();
				for (AccessControlEntry acl : acls) {
					//Do not garbage your own shares
					if(acl.subject.equalsIgnoreCase(de.uid) != owned) {
						userInfos = resolvedSubject(acl.subject, acl.verb.toString(), domainUid);
					}
				}
				aclsNumbers += userInfos.size();
				if(!userInfos.isEmpty()) {
					map.put(containerDescriptor, userInfos);
				}
			}
		}
		display(map, aclsNumbers, domainUid);
	}
	
	
	private void display(Map<ContainerDescriptor, Map<DirEntry, String>> map, int size, String domainUid) {
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
		for (Map.Entry<ContainerDescriptor, Map<DirEntry,String>> entry : map.entrySet()) {
			ContainerDescriptor containerInfos = entry.getKey();
			for (Map.Entry<DirEntry, String> member : entry.getValue().entrySet()) {
				asTable[i][0] = containerInfos.ownerDisplayname;
				asTable[i][1] = containerInfos.owner;
				asTable[i][2] = containerInfos.name;
				asTable[i][3] = containerInfos.uid;
				asTable[i][4] = containerInfos.type;
				asTable[i][5] = String.format("%s %s", member.getKey().kind.name(), member.getKey().displayName);
				asTable[i][6] = member.getKey().entryUid;
				asTable[i][7] = member.getValue();
				i++;
			}
		}
		ctx.info(AsciiTable.getTable(asTable));
	}
	
	
	private Map<DirEntry, String> resolvedSubject(String subject, String verb, String domainUid) {
		Map<DirEntry, String> map = new HashMap<DirEntry, String>();
		IDirectory dirApi = ctx.adminApi().instance(IDirectory.class, domainUid);
		DirEntry entry = dirApi.findByEntryUid(subject);
		
		if(entry == null) {
			DirEntry exception = new DirEntry();
			exception.displayName = "NO ENTRY FOUND";
			exception.entryUid = subject;
			map.put(exception, verb);
		} else if(expand && entry.kind == Kind.GROUP){
			map = expandGroup(entry, verb, domainUid);
		} else {
			map.put(entry, verb);
		}
		return map;
	}

	private Map<DirEntry,String> expandGroup(DirEntry group, String verb, String domainUid){
		
		Map<DirEntry, String> map = new HashMap<DirEntry, String>();
		IGroup groupApi = ctx.adminApi().instance(IGroup.class, domainUid);
		List<Member> members = groupApi.getExpandedUserMembers(group.entryUid);
		
		for (Member member : members) {	
			for (Map.Entry<DirEntry, String> user : resolvedSubject(member.uid, verb, domainUid).entrySet()) {
				map.put(user.getKey(), verb);
			}
		}
		return map;
	}
	
	
	
	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}

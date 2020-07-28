/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.group.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistence.GroupStore;

public class ContainerGroupStoreService extends DirValueStoreService<Group> {

	public static class GroupDirEntryAdapter implements DirEntryAdapter<Group> {

		@Override
		public DirEntry asDirEntry(String domainUid, String uid, Group group) {
			Email dEmail = group.defaultEmail();
			return DirEntry.create(group.orgUnitUid, domainUid + "/groups/" + uid, DirEntry.Kind.GROUP, uid, group.name,
					dEmail != null ? dEmail.address : null, group.hidden, group.system, group.archived,
					group.dataLocation);
		}

	}

	private GroupStore groupStore;
	private ItemStore realItemStore;

	public ContainerGroupStoreService(BmContext context, Container container, ItemValue<Domain> domain) {
		this(context, context.getDataSource(), context.getSecurityContext(), container, domain);
	}

	public ContainerGroupStoreService(BmContext context, DataSource dataSource, SecurityContext securityContext,
			Container container, ItemValue<Domain> domain) {
		super(context, dataSource, securityContext, domain, container, DirEntry.Kind.GROUP,
				new GroupStore(dataSource, container), new GroupDirEntryAdapter(),
				new GroupVCardAdapter(dataSource, securityContext, container, domain.uid), new GroupMailboxAdapter());
		this.groupStore = new GroupStore(dataSource, container);
		this.realItemStore = new ItemStore(dataSource, container, securityContext);
	}

	public Set<String> getGroupsWithRoles(List<String> roles) throws ServerFault {
		try {
			return roleStore.getItemsWithRoles(roles);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public ItemValue<DirEntryAndValue<Group>> byName(String name) throws ServerFault {
		return doOrFail(() -> {
			String uid = groupStore.byName(name);
			if (uid != null) {
				Item item = itemStore.get(uid);
				return getItemValue(item);
			} else {
				return null;
			}
		});
	}

	@Override
	public List<String> allUids() throws ServerFault {
		try {
			return groupStore.allUids();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void addMembers(String uid, List<Member> members) throws ServerFault {
		List<Member> usersMembers = new ArrayList<Member>(members.size());
		List<Member> groupsMembers = new ArrayList<Member>(members.size());
		List<Member> externalUserMembers = new ArrayList<Member>(members.size());
		for (Member member : members) {
			switch (member.type) {
			case user:
				usersMembers.add(member);
				break;
			case group:
				groupsMembers.add(member);
				break;
			case external_user:
				externalUserMembers.add(member);
				break;
			}
		}

		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				throw new ServerFault("group {}" + uid + " not found", ErrorCode.NOT_FOUND);
			}
			DirEntryAndValue<Group> value = getItemValueStore().get(item);
			if (value == null) {
				throw new ServerFault("not a group {}" + item.uid, ErrorCode.NOT_FOUND);
			}
			item = itemStore.touch(uid);
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), securityContext.getOrigin(), item.id, 0));
			}

			groupStore.addUsersMembers(item,
					realItemStore.getMultiple(usersMembers.stream().map(m -> m.uid).collect(Collectors.toList())));
			groupStore.addGroupsMembers(item,
					realItemStore.getMultiple(groupsMembers.stream().map(m -> m.uid).collect(Collectors.toList())));
			groupStore.addExternalUsersMembers(item, realItemStore
					.getMultiple(externalUserMembers.stream().map(m -> m.uid).collect(Collectors.toList())));

			vcardStore.update(item, vcardAdapter.asVCard(domain, item.uid, getItemValue(item).value.value));
			return null;
		});

	}

	public void removeMembers(String uid, List<Member> members) throws ServerFault {

		List<String> usersMembersUid = new ArrayList<String>(members.size());
		List<String> groupsMembersUid = new ArrayList<String>(members.size());
		List<String> externalUserMembersUid = new ArrayList<String>(members.size());
		for (Member member : members) {
			switch (member.type) {
			case user:
				usersMembersUid.add(member.uid);
				break;
			case group:
				groupsMembersUid.add(member.uid);
				break;
			case external_user:
				externalUserMembersUid.add(member.uid);
				break;
			}
		}
		doOrFail(() -> {
			Item item = itemStore.getForUpdate(uid);
			if (item == null) {
				throw new ServerFault("group {}" + uid + " not found", ErrorCode.NOT_FOUND);
			}
			if (getItemValueStore().get(item) == null) {
				throw new ServerFault("not a group {}" + item.uid, ErrorCode.NOT_FOUND);
			}
			item = itemStore.touch(uid);
			if (hasChangeLog) {
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), securityContext.getOrigin(), item.id, 0));
			}

			groupStore.removeUsersMembers(item,
					realItemStore.getMultiple(usersMembersUid).stream().map(i -> i.id).collect(Collectors.toList()));
			groupStore.removeGroupsMembers(item,
					realItemStore.getMultiple(groupsMembersUid).stream().map(i -> i.id).collect(Collectors.toList()));
			groupStore.removeExternalUsersMembers(item, realItemStore.getMultiple(externalUserMembersUid).stream()
					.map(i -> i.id).collect(Collectors.toList()));

			vcardStore.update(item, vcardAdapter.asVCard(domain, item.uid, getItemValue(item).value.value));
			return null;
		});

	}

	public List<Member> getMembers(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				return null;
			} else {
				return groupStore.getMembers(item);
			}
		});
	}

	public List<Member> getFlatUsersMembers(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				return null;
			} else {
				return groupStore.getFlatUsersMembers(item);
			}
		});
	}

	public List<String> getParents(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				return null;
			} else {
				return groupStore.getParents(item);
			}
		});
	}

	public boolean nameAlreadyUsed(String uid, Group group) throws ServerFault {
		if (uid == null) {
			return doOrFail(() -> {
				return groupStore.nameAlreadyUsed(group);
			});
		} else {
			return doOrFail(() -> {
				Item item = itemStore.get(uid);
				if (item == null) {
					return false;
				} else {
					return groupStore.nameAlreadyUsed(item.id, group);
				}
			});
		}
	}

	public boolean allValid(String[] uids) throws ServerFault {
		return doOrFail(() -> {
			return groupStore.areValid(uids);
		});
	}

	@Deprecated
	public void create(String uid, String displayName, Group group) throws ServerFault {
		create(uid, group);
	}

	@Deprecated
	public void update(String uid, String displayName, Group group) throws ServerFault {
		update(uid, group);
	}

	@Override
	protected byte[] getDefaultImage() {
		return DirEntryHandler.EMPTY_PNG;
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<Group>> value) throws ServerFault {
		super.decorate(item, value);
		if (value.value.mailbox != null) {
			value.value.value.emails = value.value.mailbox.emails;
			value.value.value.dataLocation = value.value.mailbox.dataLocation;
			value.value.value.orgUnitUid = value.value.entry.orgUnitUid;
		}
	}

	public List<ItemValue<DirEntryAndValue<Group>>> search(GroupSearchQuery query) throws ServerFault {
		return doOrFail(() -> {
			List<String> uids = groupStore.search(query);
			return getMultiple(uids);
		});

	}

	public List<String> getMemberOfGroup(String uid) throws ServerFault {
		return doOrFail(() -> {
			Item item = itemStore.get(uid);
			return groupStore.getGroupGroups(item);
		});
	}
}

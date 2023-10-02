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
package net.bluemind.group.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.Member;

public class GroupStore extends AbstractItemValueStore<Group> {
	private static final Logger logger = LoggerFactory.getLogger(GroupStore.class);

	private final Container container;

	private static final Creator<Group> GROUP_CREATOR = new Creator<Group>() {
		@Override
		public Group create(ResultSet con) throws SQLException {
			return new Group();
		}
	};

	private static final Creator<Member> MEMBER_CREATOR = new Creator<Member>() {
		@Override
		public Member create(ResultSet con) throws SQLException {
			return new Member();
		}
	};

	private static final Creator<Long> PARENT_CREATOR = new Creator<Long>() {
		@Override
		public Long create(ResultSet con) throws SQLException {
			return Long.valueOf(con.getLong(1));
		}
	};

	private static final Creator<String> UIDFOUND_CREATOR = new Creator<String>() {
		@Override
		public String create(ResultSet con) throws SQLException {
			return con.getString(1);
		}
	};

	private static final Creator<Integer> TOTALFOUND_CREATOR = new Creator<Integer>() {
		@Override
		public Integer create(ResultSet con) throws SQLException {
			return con.getInt(1);
		}
	};

	private static final Creator<Integer> INTEGER_CREATOR = new Creator<Integer>() {
		@Override
		public Integer create(ResultSet con) throws SQLException {
			return con.getInt(1);
		}
	};

	public GroupStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
		logger.debug("created {}", this.container);
	}

	private static final String INSERT_GROUP = "INSERT INTO t_group " //
			+ " ( " + GroupColumns.cols.names() + ", item_id, container_id)" //
			+ " VALUES "//
			+ "(" + GroupColumns.cols.values() + ", ?, ?)";

	@Override
	public void create(Item item, Group value) throws SQLException {
		insert(INSERT_GROUP, value, GroupColumns.statementValues(item, container));
		logger.debug("inserted complete: {}", value);
	}

	private static final String UPDATE_GROUP = "UPDATE t_group SET (" + GroupColumns.cols.names() + ") " //
			+ " = (" + GroupColumns.cols.values() + ") " //
			+ " WHERE item_id = ? and container_id = ? ";

	@Override
	public void update(Item item, Group value) throws SQLException {
		update(UPDATE_GROUP, value, GroupColumns.statementValues(item, container));
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_group_groupmember WHERE group_parent_id = ?", new Object[] { item.id });
		// FIXME we should not
		delete("DELETE FROM t_group_groupmember WHERE group_child_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_group_usermember WHERE group_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_group_externalusermember WHERE group_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_group_flat_members WHERE group_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_group WHERE item_id = ?", new Object[] { item.id });
	}

	private static final String SELECT_GROUP = "SELECT " + GroupColumns.cols.names("g")
			+ ", (SELECT count(group_id) FROM t_group_flat_members WHERE group_id = item.id )"
			+ " FROM t_group g, t_container_item item " //
			+ " WHERE " //
			+ " g.item_id = item.id AND " //
			+ " g.item_id = ?";

	@Override
	public Group get(Item item) throws SQLException {
		Group g = unique(SELECT_GROUP, GROUP_CREATOR, GroupColumns.populator(), new Object[] { item.id });
		if (g == null) {
			return null;
		}

		return g;
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_group_groupmember WHERE group_parent_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });

		delete("DELETE FROM t_group_usermember WHERE group_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });

		delete("DELETE FROM t_group_externalusermember WHERE group_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });

		delete("DELETE FROM t_group_flat_members WHERE group_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });

		delete("DELETE FROM t_group WHERE item_id IN (SELECT id FROM t_container_item WHERE container_id = ?)",
				new Object[] { container.id });
	}

	public void addUsersMembers(Item item, List<Item> usersMembers) throws SQLException {
		if (usersMembers == null || usersMembers.isEmpty()) {
			return;
		}

		batchInsert("""
				INSERT INTO t_group_usermember (group_id, user_id)
				VALUES (?, ?)
				ON CONFLICT (group_id, user_id) DO NOTHING
				""", usersMembers, MemberColumns.statementValues(item));

		updateUserGroupHierarchy(item, usersMembers.stream().map(i -> i.id).collect(Collectors.toSet()),
				Collections.emptySet());
	}

	public void addGroupsMembers(Item groupItem, List<Item> groupsMembers) throws SQLException, ServerFault {
		if (groupsMembers == null || groupsMembers.isEmpty()) {
			return;
		}

		detectLoop(groupItem, groupsMembers);

		batchInsert("""
				INSERT INTO t_group_groupmember (group_parent_id, group_child_id)
				VALUES (?, ?)
				ON CONFLICT (group_parent_id, group_child_id) DO NOTHING
				""", groupsMembers, (con, statement, index, currentRow, value) -> {
			statement.setLong(index++, groupItem.id);
			statement.setLong(index++, value.id);
			return 0;
		});
		Set<Long> flatUsersIds = allUserIdsInGroup(groupItem.id);
		updateUserGroupHierarchy(groupItem, flatUsersIds, Collections.emptySet());
	}

	public void addExternalUsersMembers(Item item, List<Item> externalUsersMembers) throws SQLException {
		if (externalUsersMembers == null || externalUsersMembers.isEmpty()) {
			return;
		}

		batchInsert("""
				INSERT INTO t_group_externalusermember (group_id, external_user_id)
				VALUES (?, ?)
				ON CONFLICT (group_id, external_user_id) DO NOTHING
				""", externalUsersMembers, MemberColumns.statementValues(item));

		updateUserGroupHierarchy(item, externalUsersMembers.stream().map(i -> i.id).collect(Collectors.toSet()),
				Collections.emptySet());
	}

	private void detectLoop(Item item, List<Item> groupsMembers) throws SQLException, ServerFault {
		Set<Long> parents = getParents(item.id);
		parents.add(item.id);

		Integer count = unique("SELECT count(*) FROM t_container_item WHERE id = ANY (?) AND id = ANY(?)",
				TOTALFOUND_CREATOR, Collections.emptyList(),
				new Object[] { groupsMembers.stream().map(i -> i.id).toArray(s -> new Long[s]),
						parents.toArray(new Long[0]) });

		if (count > 0) {
			throw new ServerFault("Group loop detected", ErrorCode.INCLUSION_GROUP_LOOP);
		}
	}

	public void removeUsersMembers(Item item, Collection<Long> membersIds) throws SQLException {
		if (membersIds == null || membersIds.isEmpty()) {
			return;
		}

		delete("DELETE FROM t_group_usermember WHERE group_id = ? AND user_id = ANY (?)",
				new Object[] { item.id, membersIds.toArray(new Long[membersIds.size()]) });

		updateUserGroupHierarchy(item, Collections.emptySet(), membersIds.stream().collect(Collectors.toSet()));
	}

	public void removeGroupsMembers(Item groupItem, Collection<Long> membersIds) throws SQLException {
		if (membersIds == null || membersIds.isEmpty()) {
			return;
		}

		delete("DELETE FROM t_group_groupmember WHERE group_parent_id = ? AND group_child_id = ANY (?)",
				new Object[] { groupItem.id, membersIds.toArray(new Long[membersIds.size()]) });

		Set<Long> flatUsersIds = allUserIdsInGroup(groupItem.id);
		updateUserGroupHierarchy(groupItem, Collections.emptySet(), flatUsersIds);
	}

	public void removeExternalUsersMembers(Item item, Collection<Long> membersIds) throws SQLException {
		if (membersIds == null || membersIds.isEmpty()) {
			return;
		}

		delete("DELETE FROM t_group_externalusermember WHERE group_id = ? AND external_user_id = ANY (?)",
				new Object[] { item.id, membersIds.toArray(new Long[membersIds.size()]) });

		updateUserGroupHierarchy(item, Collections.emptySet(), membersIds.stream().collect(Collectors.toSet()));
	}

	private static final String SELECT_MEMBERS = """
			SELECT 'user', t_container_item.uid AS uid
			    FROM t_group_usermember
			    JOIN t_container_item ON t_container_item.id = user_id
			    WHERE group_id = ?
			UNION
			SELECT 'external_user', t_container_item.uid AS uid
			    FROM t_group_externalusermember
			    JOIN t_container_item ON t_container_item.id = external_user_id
			    WHERE group_id = ?
			UNION
			SELECT 'group', t_container_item.uid AS uid
			    FROM t_group_groupmember
			    INNER JOIN t_container_item ON t_container_item.id = group_child_id
			    WHERE group_parent_id = ?
			ORDER BY 1, 2
			""";

	public List<Member> getMembers(Item item) throws SQLException {
		return select(SELECT_MEMBERS, MEMBER_CREATOR, MemberColumns.populator(),
				new Object[] { item.id, item.id, item.id });
	}

	/**
	 * Update the selected group, AND the parent groups
	 * 
	 * @param groupItem:      the group to update (and potential parent groups)
	 * @param addedUserIds:   t_container_item(id) of added users
	 * @param removedUserIds: t_container_item(id) of removed users
	 * @throws SQLException
	 */
	private void updateUserGroupHierarchy(Item groupItem, Set<Long> addedUserIds, Set<Long> removedUserIds)
			throws SQLException {
		logger.debug("Updating t_group_flat_members for group {}", groupItem.id);

		if (!addedUserIds.isEmpty()) {
			addFlatMembers(groupItem.id, addedUserIds);
		}
		if (!removedUserIds.isEmpty()) {
			removeFlatMembers(groupItem.id, removedUserIds);
		}

		Set<Long> parentGroupIds = getParents(groupItem.id);
		for (Long parentGroupId : parentGroupIds) {
			logger.debug("Updating t_group_flat_members for parent group id {} of group id {}", parentGroupId,
					groupItem.id);
			if (!addedUserIds.isEmpty()) {
				addFlatMembers(parentGroupId, addedUserIds);
			}
			if (!removedUserIds.isEmpty()) {
				removeFlatMembers(parentGroupId, removedUserIds);
			}
		}
	}

	private void addFlatMembers(Long groupItemId, Set<Long> users) throws SQLException {
		if (!users.isEmpty()) {
			batchInsert("""
					INSERT INTO t_group_flat_members (group_id, user_id)
					VALUES (?, ?)
					ON CONFLICT DO NOTHING
					""", users, (con, statement, index, currentRow, value) -> {
				statement.setLong(index++, groupItemId);
				statement.setLong(index++, value);
				return 0;
			});
		}
	}

	private void removeFlatMembers(Long groupItemId, Set<Long> removedUserId) throws SQLException {
		if (!removedUserId.isEmpty()) {
			delete("""
					DELETE FROM t_group_flat_members
					WHERE group_id = ? AND user_id = ANY(?)
					""",
					new Object[] { groupItemId, removedUserId.stream().toArray(s -> new Long[removedUserId.size()]) });
		}
	}

	private Set<Long> allUserIdsInGroup(Long groupItemId) throws SQLException {
		List<Long> childs = getChildren(groupItemId);
		childs.add(Long.valueOf(groupItemId));

		Set<Long> members = new HashSet<>(selectLong("""
				SELECT user_id
				FROM t_group_usermember
				WHERE group_id = ANY (?)
				ORDER BY user_id
				""", new Object[] { childs.toArray(new Long[childs.size()]) }));

		members.addAll(selectLong("""
				SELECT external_user_id
				FROM t_group_externalusermember
				WHERE group_id = ANY (?)
				ORDER BY external_user_id
				""", new Object[] { childs.toArray(new Long[childs.size()]) }));

		return members;
	}

	private List<Long> getChildren(Long groupItemId) throws SQLException {
		List<Long> childrens = selectLong("""
				WITH RECURSIVE children(group_child_id, group_parent_id) AS (
				    SELECT group_child_id, group_parent_id
				    FROM t_group_groupmember
				    WHERE group_parent_id = ?
				    UNION ALL
				    SELECT p.group_child_id, p.group_parent_id
				    FROM children pr, t_group_groupmember p
				    WHERE p.group_parent_id = pr.group_child_id
				)
				SELECT group_child_id
				FROM children
				""", new Object[] { groupItemId });

		logger.debug("Found {} child group for group {}", childrens.size(), groupItemId);
		return childrens;
	}

	public List<String> getParents(Item item) throws SQLException {
		Set<Long> parents = getParents(item.id);

		return select("SELECT uid FROM t_container_item WHERE id = ANY(?) ORDER BY uid", UIDFOUND_CREATOR,
				Collections.emptyList(), new Object[] { parents.toArray(new Long[0]) });

	}

	private Set<Long> getParents(Long groupItemId) throws SQLException {
		List<Long> parents = selectLong("""
				WITH RECURSIVE parents(group_child_id, group_parent_id) AS (
				    SELECT group_child_id, group_parent_id
				        FROM t_group_groupmember
				        WHERE group_child_id = ?
				    UNION ALL
				    SELECT p.group_child_id, p.group_parent_id
				        FROM parents pr, t_group_groupmember p
				        WHERE p.group_child_id = pr.group_parent_id
				) SELECT group_parent_id FROM parents
				""", new Object[] { groupItemId });

		logger.debug("Found {} parents group for group {}", parents.size(), groupItemId);

		return new HashSet<>(parents);
	}

	public List<Member> getFlatUsersMembers(Item item) throws SQLException {
		return select("""
				SELECT 'user', ui.uid
				    FROM t_group_flat_members m
				    INNER JOIN t_container_item ui ON ui.id = m.user_id
				    INNER JOIN t_group_usermember um ON um.user_id = m.user_id
				    WHERE m.group_id = ?
				UNION
				SELECT 'external_user', ui.uid
				    FROM t_group_flat_members member
				    INNER JOIN t_container_item ui ON ui.id = member.user_id
				    INNER JOIN t_group_externalusermember eum ON eum.external_user_id = member.user_id
				    WHERE member.group_id = ?
				ORDER BY 1, 2
				""", MEMBER_CREATOR, MemberColumns.populator(), new Object[] { item.id, item.id });

	}

	public List<String> getUserGroups(Container userContainer, Item item) throws SQLException {
		return select("""
				SELECT item.uid
				FROM t_group g
				INNER JOIN t_container_item item ON g.item_id = item.id
				INNER JOIN t_group_flat_members member ON g.item_id = member.group_id
				INNER JOIN t_container_item memberitem ON member.user_id = memberitem.id
				WHERE member.user_id = ?
				AND memberitem.container_id = ?
				ORDER BY item.uid
				""", UIDFOUND_CREATOR, Collections.emptyList(), new Object[] { item.id, userContainer.id });
	}

	public List<String> getGroupGroups(Item item) throws SQLException {
		return select("""
				SELECT item.uid
				FROM t_group g
				INNER JOIN t_container_item item ON g.item_id = item.id
				INNER JOIN t_group_groupmember member ON g.item_id = member.group_parent_id
				INNER JOIN t_container_item memberitem ON member.group_child_id = memberitem.id
				WHERE member.group_child_id = ?
				AND memberitem.container_id = ?
				ORDER BY item.uid
				""", UIDFOUND_CREATOR, Collections.emptyList(), new Object[] { item.id, container.id });
	}

	public boolean areValid(String[] groupsUids) throws SQLException {
		String query = """
				SELECT count(*)
				FROM t_group g
				JOIN t_container_item i ON i.id = g.item_id
				WHERE i.container_id = ?
				AND i.uid = ANY(?)
				""";
		int count = unique(query, INTEGER_CREATOR, new ArrayList<EntityPopulator<Integer>>(0),
				new Object[] { container.id, groupsUids });

		return count == groupsUids.length;
	}

	public boolean nameAlreadyUsed(Group group) throws SQLException {
		return nameAlreadyUsed(null, group);
	}

	private static final String NAME_ALREADY_EXISTS = """
			SELECT count(*)
			FROM t_group
			WHERE container_id = ?
			AND LOWER(name) = LOWER(?)
			""";
	private static final String NAME_ALREADY_EXISTS_WITH_ITEM = NAME_ALREADY_EXISTS + " AND item_id != ?";

	public boolean nameAlreadyUsed(Long itemId, Group group) throws SQLException {
		if (group == null) {
			return false;
		}

		Object[] parameters = null;
		String query = null;
		if (itemId != null) {
			query = NAME_ALREADY_EXISTS_WITH_ITEM;
			parameters = new Object[] { container.id, group.name, itemId };
		} else {
			query = NAME_ALREADY_EXISTS;
			parameters = new Object[] { container.id, group.name };
		}

		Integer total = unique(query, TOTALFOUND_CREATOR, Collections.emptyList(), parameters);

		return (total != 0);
	}

	public static final String SELECT_BY_NAME = """
			SELECT item.uid
			FROM t_group g, t_container_item item
			WHERE item.id = g.item_id
			AND item.container_id = ?
			AND name = ?
			ORDER BY item.uid
			""";

	public String byName(String name) throws SQLException {
		return unique(SELECT_BY_NAME, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, name });
	}

	public static final String SELECT_ALL = """
			SELECT item.uid
			FROM t_group g, t_container_item item
			WHERE item.id = g.item_id
			AND item.container_id = ?
			""";

	public List<String> allUids() throws SQLException {
		return select(SELECT_ALL, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id });

	}

	public List<String> search(GroupSearchQuery query) throws SQLException {
		List<Object> params = new ArrayList<>();
		StringBuilder search = new StringBuilder(SELECT_ALL);
		params.add(container.id);
		if (!Strings.isNullOrEmpty(query.name)) {
			search.append(" AND name ilike ?");
			params.add("%" + query.name + "%");
		}
		if (!query.properties.isEmpty()) {
			search.append(" AND properties -> ? = ?");
			params.add(query.properties.keySet().toArray(new String[query.properties.size()]));
			params.add(query.properties.values().toArray(new String[query.properties.size()]));
		}
		return select(search.toString(), StringCreator.FIRST, Collections.emptyList(), params.toArray());
	}
}

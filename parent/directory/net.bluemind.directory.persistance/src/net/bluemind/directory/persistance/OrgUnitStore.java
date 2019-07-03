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
package net.bluemind.directory.persistance;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.AbstractItemValueStore;
import net.bluemind.core.container.persistance.IntegerCreator;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.container.persistance.StringCreator;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.persistance.internal.OrgUnitColumns;

public class OrgUnitStore extends AbstractItemValueStore<OrgUnit> {

	private static final Creator<OrgUnit> CREATOR = con -> new OrgUnit();
	private Container dirContainer;
	private ItemStore itemStore;

	public OrgUnitStore(DataSource dataSource, Container dirContainer) {
		super(dataSource);
		itemStore = new ItemStore(dataSource, dirContainer, null);
		this.dirContainer = dirContainer;
	}

	public static final String INSERT_QUERY = "INSERT INTO t_directory_ou " //
			+ " ( name, parent_item_id, item_id) VALUES " //
			+ " ( ?, ?, ?) ";

	@Override
	public void create(Item item, OrgUnit value) throws SQLException {
		Long parentId = null;
		if (value.parentUid != null) {
			Item parent = itemStore.get(value.parentUid);
			if (parent == null) {
				throw new ServerFault("parent " + value.parentUid + " not found", ErrorCode.NOT_FOUND);
			} else {
				parentId = parent.id;
			}
		}

		insert(INSERT_QUERY, new Object[] { value.name, parentId, item.id });
	}

	public static final String UPDATE_QUERY = //
			"UPDATE t_directory_ou SET (name) = ROW( ? ) "//
					+ " WHERE item_id = ? ";

	@Override
	public void update(Item item, OrgUnit value) throws SQLException {
		update(UPDATE_QUERY, new Object[] { value.name, item.id });
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_directory_ou_administrator WHERE ou_id = ?", new Object[] { item.id });
		delete("DELETE FROM t_directory_ou WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public OrgUnit get(Item item) throws SQLException {
		String query = "SELECT ou.name, parentOu.uid "
				+ " FROM t_directory_ou ou left outer join t_container_item parentOu on parentOu.id = ou.parent_item_id " //
				+ " WHERE " //
				+ " ou.item_id = ? ";

		return unique(query, CREATOR, OrgUnitColumns.populator(), new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		throw new ServerFault("Not implemented", ErrorCode.DEPRECATED);
	}

	public OrgUnitPath getPath(Item item) throws SQLException {
		String query = //
				"WITH RECURSIVE parts(item_id, parentId, name, uid) AS ( " //
						+ "     SELECT item_id, parent_item_id, name, i.uid from t_directory_ou ou, t_container_item i where i.id = ou.item_id and i.id = ? "
						+ " UNION ALL "
						+ "     SELECT pou.item_id, pou.parent_item_id, pou.name, pi.uid from t_directory_ou pou, t_container_item pi, parts where pi.id = pou.item_id and pou.item_id = parts.parentId "
						+ " )" + " SELECT array_agg(uid), array_agg(name) from parts ";

		return unique(query, pathCreator(), Collections.emptyList(), new Object[] { item.id });
	}

	public OrgUnitPath getPathByUid(String ouUid) throws SQLException {
		String query = //
				"WITH RECURSIVE parts(item_id, parentId, name, uid) AS ( " //
						+ "     SELECT item_id, parent_item_id, name, i.uid from t_directory_ou ou, t_container_item i where i.id = ou.item_id and i.uid = ? "
						+ " UNION ALL "
						+ "     SELECT pou.item_id, pou.parent_item_id, pou.name, pi.uid from t_directory_ou pou, t_container_item pi, parts where pi.id = pou.item_id and pou.item_id = parts.parentId "
						+ " )" + " SELECT array_agg(uid), array_agg(name) from parts ";

		return unique(query, pathCreator(), Collections.emptyList(), new Object[] { ouUid });
	}

	private Creator<OrgUnitPath> pathCreator() {
		return new Creator<OrgUnitPath>() {

			@Override
			public OrgUnitPath create(ResultSet rs) throws SQLException {
				int index = 1;
				String[] uids = arrayOfString(rs.getArray(index++));
				String[] names = arrayOfString(rs.getArray(index++));

				if (uids.length == 0) {
					return null;
				}

				OrgUnitPath cur = null;

				for (int p = uids.length - 1; p >= 0; p--) {
					if (cur == null) {
						cur = new OrgUnitPath();
					} else {
						OrgUnitPath parent = cur;
						cur = new OrgUnitPath();
						cur.parent = parent;
					}
					cur.uid = uids[p];
					cur.name = names[p];
				}

				return cur;
			}
		};
	}

	private static String[] arrayOfString(Array array) throws SQLException {
		String[] ret = null;
		if (array != null) {
			ret = (String[]) array.getArray();
		} else {
			ret = new String[0];// FIXME length
		}
		return ret;
	}

	public List<String> search(String name) throws SQLException {
		return search(name, null);
	}

	public List<String> search(String name, List<String> limitToOu) throws SQLException {
		String wquery = //
				"WITH RECURSIVE parts(item_id, ancestors, path, parent_item_id) AS ( " //
						+ "     SELECT item_id, array[uid]::text[], name,  parent_item_id " //
						+ "		FROM t_directory_ou ou" //
						+ "		JOIN t_container_item i ON i.id = ou.item_id WHERE" //
						+ "		ou.parent_item_id IS NULL " //
						+ " UNION ALL " //
						+ "     SELECT pou.item_id, parts.ancestors || pi.uid, parts.path || '/' || pou.name, pou.parent_item_id " //
						+ "     FROM t_directory_ou pou" //
						+ "		JOIN t_container_item pi ON pi.id = pou.item_id" //
						+ "		JOIN parts ON pou.parent_item_id = parts.item_id " //
						+ " )";

		String query = wquery //
				+ " SELECT m.uid FROM t_directory_ou ou " //
				+ " JOIN t_container_item m ON ou.item_id = m.id " //
				+ " JOIN parts ON ou.item_id = parts.item_id " //
				+ " WHERE m.container_id = ? AND parts.path ILIKE ? ";
		ArrayList<Object> params = new ArrayList<>();
		params.add(dirContainer.id);
		params.add("%" + name + "%");
		if (limitToOu != null) {
			query += " AND ? && ancestors";
			params.add(limitToOu.toArray(new String[0]));
		}
		query += " ORDER BY parts.path ";
		return select(query, StringCreator.FIRST, Collections.emptyList(), params.toArray(new Object[0]));
	}

	public boolean pathExists(String name, String parent) throws SQLException {
		int count;
		if (parent != null && !parent.isEmpty()) {
			String query = "SELECT COUNT(*) FROM t_directory_ou ou"
					+ " LEFT OUTER JOIN t_container_item parentOu ON parentOu.id = ou.parent_item_id " //
					+ " WHERE ou.name ILIKE ? AND parentOu.uid = ? AND parentOu.container_id = ? ";

			count = unique(query, new IntegerCreator(1), Collections.emptyList(),
					new Object[] { name, parent, dirContainer.id });
		} else {
			String query = "SELECT COUNT(*) "
					+ " FROM t_directory_ou ou INNER JOIN t_container_item item ON ou.item_id = item.id "
					+ " WHERE ou.name ILIKE ? AND ou.parent_item_id IS NULL AND item.container_id = ? ";

			count = unique(query, new IntegerCreator(1), Collections.emptyList(),
					new Object[] { name, dirContainer.id });
		}
		return count > 0;

	}

	public void setAdminRoles(Item ouItem, Item adminItem, Set<String> roles) throws SQLException {
		delete("DELETE FROM t_directory_ou_administrator WHERE ou_id = ? AND administrator_item_id = ?",
				new Object[] { ouItem.id, adminItem.id });
		if (!roles.isEmpty()) {
			insert("INSERT INTO t_directory_ou_administrator (ou_id, administrator_item_id, roles) VALUES ( ? , ? , ?::text[])",
					new Object[] { ouItem.id, adminItem.id, roles.toArray(new String[0]) });
		}
	}

	public Set<String> getAdminRoles(Item ouItem, List<Item> admins) throws SQLException {
		String[] roles = unique(
				"SELECT roles FROM t_directory_ou_administrator WHERE ou_id = ? AND administrator_item_id = ANY(?) ",
				rs -> {
					Array array = rs.getArray(1);
					if (array != null) {
						return (String[]) array.getArray();
					} else {
						return null;
					}
				}, Arrays.asList(),
				new Object[] { ouItem.id, admins.stream().map(admin -> admin.id).toArray(size -> new Long[size]) });

		if (roles == null) {
			return Collections.emptySet();
		} else {
			return new HashSet<>(Arrays.asList(roles));
		}
	}

	public Set<String> getAdministrators(Item ouItem) throws SQLException {
		List<String> admins = select(
				"SELECT item.uid FROM t_container_item item, t_directory_ou_administrator WHERE ou_id = ? AND item.id = administrator_item_id ",
				StringCreator.FIRST, Collections.emptyList(), new Object[] { ouItem.id });
		if (admins == null) {
			return null;
		} else {
			return new HashSet<>(admins);
		}
	}

	public void removeAdministrator(String uid) throws SQLException {
		delete("DELETE from t_directory_ou_administrator where administrator_item_id = ANY (select id from t_container_item where uid = ?)",
				new Object[] { uid });
	}

	public List<String> listByAdministrator(List<Item> items) throws SQLException {
		return select(
				"SELECT item.uid FROM t_container_item item, t_directory_ou ou, t_directory_ou_administrator WHERE ou.item_id = ou_id AND item.id = ou.item_id AND administrator_item_id = ANY(?) ",
				StringCreator.FIRST, Collections.emptyList(),
				new Object[] { items.stream().map(item -> item.id).toArray(size -> new Long[size]) });
	}

	public List<String> getMembers(Item item) throws SQLException {
		String query = //
				"WITH RECURSIVE parts(item_id, parentId) AS ( " //
						+ "     SELECT item_id, parent_item_id from t_directory_ou ou where ou.item_id = ?"
						+ " UNION ALL "
						+ "     SELECT pou.item_id, pou.parent_item_id from t_directory_ou pou, parts where pou.parent_item_id = parts.item_id "
						+ " )" //
						+ "SELECT m.uid FROM t_directory_ou_member, t_container_item m WHERE "
						+ " t_directory_ou_member.ou_id in ( select item_id from parts) AND m.id = t_directory_ou_member.member_item_id ";
		return select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { item.id });
	}

	public boolean hasChilds(Item item) throws SQLException {
		Integer count = unique("SELECT COUNT(*) FROM t_directory_ou WHERE parent_item_id = ? ", res -> {
			return new Integer(res.getInt(1));
		}, Collections.emptyList(), new Object[] { item.id });
		return count > 0;
	}

	public List<String> getChildren(Item item) throws SQLException {
		return select(
				"SELECT item.uid FROM t_container_item item join t_directory_ou o on o.item_id = item.id WHERE o.parent_item_id = ? ",
				StringCreator.FIRST, Collections.emptyList(), new Object[] { item.id });
	}

	public boolean hasAdministrator(Item item) throws SQLException {
		Integer count = unique("SELECT COUNT(*) FROM t_directory_ou_administrator WHERE ou_id = ? ", res -> {
			return new Integer(res.getInt(1));
		}, Collections.emptyList(), new Object[] { item.id });
		return count > 0;
	}

	public boolean hasMembers(Item ouItem) throws SQLException {
		Integer count = unique("SELECT COUNT(*) FROM t_directory_entry WHERE orgunit_item_id = ? ", res -> {
			return new Integer(res.getInt(1));
		}, Collections.emptyList(), new Object[] { ouItem.id });
		return count > 0;
	}

}

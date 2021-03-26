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
package net.bluemind.directory.persistence;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
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

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.AbstractItemValueStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.persistence.StringCreator;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.StateFilter;
import net.bluemind.directory.persistence.internal.DirEntryColumns;
import net.bluemind.directory.persistence.internal.IntegerCreator;

public class DirEntryStore extends AbstractItemValueStore<DirEntry> {

	private static final Logger logger = LoggerFactory.getLogger(DirEntryStore.class);

	private static final Creator<DirEntry> ENTRY_CREATOR = con -> new DirEntry();

	private Container container;

	public DirEntryStore(DataSource pool, Container container) {
		super(pool);
		this.container = container;
	}

	private static final String PARAMETER_Q = "WITH "
			+ "qp AS ( SELECT ? kind, ?::t_directory_entry_account_type account_type, ? entry_uid, ? displayname , ? email, ? flag_hidden, ? flag_system, ?  flag_archived, ? datalocation) "
			+ ", orgUnit AS ( SELECT ou.item_id orgId FROM t_container_item dou, t_directory_ou ou WHERE dou.id = ou.item_id AND dou.uid =  ?)";

	private static final String PARAMETER_C = "qp.kind, qp.account_type, qp.entry_uid, qp.displayname, qp.email, qp.flag_hidden, qp.flag_system, qp.flag_archived, qp.datalocation, orgUnit.orgId ";

	private static final String UPD_PARAMETER_Q = "WITH "
			+ "qp AS ( SELECT ? kind, ? entry_uid, ? displayname , ? email, ? flag_hidden, ? flag_system, ?  flag_archived, ? datalocation) "
			+ ", orgUnit AS ( SELECT ou.item_id orgId FROM t_container_item dou, t_directory_ou ou WHERE dou.id = ou.item_id AND dou.uid =  ?)";

	private static final String UPD_PARAMETER_C = "qp.kind, qp.entry_uid, qp.displayname, qp.email, qp.flag_hidden, qp.flag_system, qp.flag_archived, qp.datalocation, orgUnit.orgId ";

	@Override
	public void create(Item item, DirEntry entry) throws SQLException {
		logger.debug("create direntry for item {} ", item.id);

		String query = PARAMETER_Q + "INSERT INTO t_directory_entry ( " + "" + DirEntryColumns.COLUMNS_MAIN.names()
				+ ", item_id) " + "SELECT " + PARAMETER_C + ", ? FROM qp left outer join orgUnit on TRUE ";
		insert(query, entry, DirEntryColumns.values(item));
	}

	@Override
	public void update(Item item, DirEntry value) throws SQLException {
		logger.debug("update direntroy for item {} ", item.id);

		String query = UPD_PARAMETER_Q + "UPDATE t_directory_entry SET ( " + DirEntryColumns.UPD_COLUMNS_MAIN.names()
				+ ") = (" + UPD_PARAMETER_C + ")  FROM qp left outer join orgUnit on TRUE " + " WHERE item_id = ? ";

		update(query, value, DirEntryColumns.updValues(item));
	}

	@Override
	public DirEntry get(Item item) throws SQLException {

		String query = "SELECT " + DirEntryColumns.COLUMNS_MAIN.names("dir")
				+ ", ou.uid FROM t_directory_entry dir left outer join t_container_item ou ON ou.id = dir.orgunit_item_id  WHERE item_id = ?";

		return unique(query, ENTRY_CREATOR, DirEntryColumns.populator(container.domainUid), new Object[] { item.id });
	}

	private static final String[] ALL_KIND = Arrays.stream(Kind.values()).map(k -> k.name())
			.toArray(i -> new String[i]);

	public List<String> path(String path) throws SQLException {
		String[] comps = path.split("/");
		String[] kinds = null;
		if (comps.length == 3) {
			// {domainUid}/{kind}/{entryUid}
			String r = byEntryUid(comps[2]);
			if (r == null) {
				return Collections.emptyList();
			} else {
				return Arrays.asList(r);
			}
		} else if (comps.length == 0 || comps.length == 1) {
			// path == {domainUid}
			kinds = ALL_KIND;
		} else if ("users".equals(comps[1])) {
			// path == {domainUid}/users
			kinds = new String[] { Kind.USER.name() };
		} else if ("groups".equals(comps[1])) {
			// path == {domainUid}/groups
			kinds = new String[] { Kind.GROUP.name() };
		} else if ("resources".equals(comps[1])) {
			// path == {domainUid}/resources
			kinds = new String[] { Kind.RESOURCE.name() };
		} else if ("addressbooks".equals(comps[1])) {
			// path == {domainUid}/addressbooks
			kinds = new String[] { Kind.ADDRESSBOOK.name() };
		} else if ("calendars".equals(comps[1])) {
			// path == {domainUid}/calendars
			kinds = new String[] { Kind.CALENDAR.name() };
		} else if ("mailshares".equals(comps[1])) {
			// path == {domainUid}/mailshares
			kinds = new String[] { Kind.MAILSHARE.name() };
		} else if ("ous".equals(comps[1])) {
			// path == {domainUid}/ous
			kinds = new String[] { Kind.ORG_UNIT.name() };
		}
		String query = "select item.uid from t_container_item item, t_directory_entry dir"
				+ " WHERE item.id = dir.item_id AND item.container_id = ? " //
				+ " AND dir.kind = ANY ( ? )";

		List<String> itemUids = select(query, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, kinds });

		return itemUids;

	}

	public String byEntryUid(String entryUid) throws SQLException {
		String query = "select item.uid from t_container_item item, t_directory_entry dir"
				+ " WHERE item.id = dir.item_id AND item.container_id = ? " + " AND dir.entry_uid = ?";

		return unique(query, StringCreator.FIRST, Collections.emptyList(), new Object[] { container.id, entryUid });
	}

	private static final String BY_EMAIL_QUERY = "SELECT item.uid FROM t_container_item item" //
			+ " JOIN t_directory_entry dir ON item.id = dir.item_id" //
			+ " WHERE item.container_id = ? AND dir.email = ?" //
			+ " UNION " //
			+ " SELECT item2.uid FROM t_container_item item2" //
			+ " JOIN (" //
			+ " SELECT * FROM t_mailbox_email WHERE left_address = ? AND ( right_address = ? OR (all_aliases AND ?))" //
			+ " ) e ON item2.id = e.item_id" //
			+ " WHERE item2.container_id = ? LIMIT 1";

	public String byEmail(String email, boolean isDomainAlias) throws SQLException {
		String[] splittedEmail = email.split("@");
		return unique(BY_EMAIL_QUERY, StringCreator.FIRST, Collections.emptyList(),
				new Object[] { container.id, email, splittedEmail[0], splittedEmail[1], isDomainAlias, container.id });
	}

	public ListResult<Item> search(DirEntryQuery q) throws SQLException {
		unaccent(q);
		String withQuery = "WITH qp AS ( SELECT (?::text[]) as kind, (?::t_directory_entry_account_type) as account_type, "
				+ "(?::text) as name, (?::text) as nameOrEmail,(?::text) as nameOrEmailSplitted, (?::text) as email, (?::text) as emailLeftPart, "
				+ "(?::text) as hidden, (?::text) as system ,(?::text[]) as entryuid, (?::boolean) as archived, "
				+ "(?::text) as datalocation) ";

		String baseQuery = " from t_container_item item, t_directory_entry dir, qp "
				+ " WHERE item.id = dir.item_id AND item.container_id = ? "
				+ " AND ( qp.kind is null or dir.kind = ANY ( qp.kind ))" //
				+ " AND ( qp.account_type is null or dir.account_type = qp.account_type )" //
				+ " AND ( qp.name is null or unaccent(dir.displayname) ilike qp.name) " //
				+ " AND ( qp.nameOrEmail is null or (dir.email ilike qp.nameOrEmail or unaccent(dir.displayname) ilike qp.nameOrEmail)) " //
				+ " AND ( qp.email is null or dir.email ilike qp.email) " //
				+ " AND ( qp.hidden is null or dir.flag_hidden = false )"
				+ " AND ( qp.system is null or dir.flag_system = false )"
				+ " AND ( qp.entryuid is null or item.uid = ANY ( entryuid ))" //
				+ " AND ( qp.archived is null or dir.flag_archived = qp.archived )" //
				+ " AND ( qp.datalocation is null or dir.datalocation = qp.datalocation )";

		String countQuery = withQuery + " select count(item_id) " + baseQuery;

		String selectQuery = withQuery + " select " + ItemStore.COLUMNS.names("item") + baseQuery + " order by ";

		if (q.order == null) {
			q.order = DirEntryQuery.defaultOrder();
		}

		switch (q.order.by) {
		case displayname:
			selectQuery += "dir.displayname";
			break;
		case kind:
		default:
			selectQuery += "dir.kind";
			break;
		}

		switch (q.order.dir) {
		case desc:
			selectQuery += " desc";
			break;
		case asc:
		default:
			selectQuery += " asc";
			break;
		}
		selectQuery += " limit ? offset ?";

		ArrayList<Object> params = new ArrayList<Object>();
		if (q.kindsFilter != null && q.kindsFilter.size() > 0) {
			String[] kinds = new String[q.kindsFilter.size()];

			int i = 0;
			for (Kind k : q.kindsFilter) {
				kinds[i] = k.name();
				i++;
			}
			params.add(kinds);

		} else {
			params.add(null);
		}

		if (q.accountTypeFilter != null) {
			params.add(q.accountTypeFilter.name());
		} else {
			params.add(null);
		}

		if (q.nameFilter != null) {
			params.add("%" + q.nameFilter + "%");
		} else {
			params.add(null);
		}

		if (q.nameOrEmailFilter != null) {
			String splitted = q.nameOrEmailFilter;
			if (splitted.indexOf('@') > 0) {
				splitted = splitted.substring(0, splitted.indexOf('@'));
			}

			params.add("%" + q.nameOrEmailFilter + "%");
			params.add("%" + splitted);
		} else {
			params.add(null);
			params.add(null);
		}

		if (q.emailFilter != null) {
			params.add(q.emailFilter);
			params.add(q.emailFilter.split("@")[0]);
		} else {
			params.add(null);
			params.add(null);
		}

		if (q.hiddenFilter) {
			params.add("filterHidden");
		} else {
			params.add(null);
		}

		if (q.systemFilter) {
			params.add("filterSystem");
		} else {
			params.add(null);
		}

		if (q.entryUidFilter != null && q.entryUidFilter.size() > 0) {
			String[] entryUids = new String[q.entryUidFilter.size()];

			int i = 0;
			for (String k : q.entryUidFilter) {
				entryUids[i] = k;
				i++;
			}
			params.add(entryUids);

		} else {
			params.add(null);
		}

		if (q.stateFilter != null && q.stateFilter != DirEntryQuery.StateFilter.All) {
			if (q.stateFilter == StateFilter.Active) {
				params.add(Boolean.FALSE);
			} else {
				params.add(Boolean.TRUE);
			}
		} else {
			params.add(null);
		}

		if (!Strings.isNullOrEmpty(q.dataLocation)) {
			params.add(q.dataLocation);
		} else {
			params.add(null);
		}

		params.add(container.id);

		int count = -1;
		if (q.size <= 0) {
			selectQuery = selectQuery.replace("limit ?", "");
		} else {
			count = unique(countQuery, new IntegerCreator(1), Collections.<EntityPopulator<Integer>>emptyList(),
					params.toArray());
			params.add(q.size);
		}

		params.add(q.from);

		List<Item> items = select(selectQuery, (rs) -> new Item(), Arrays.asList(ItemStore.ITEM_POPULATOR),
				params.toArray());

		ListResult<Item> ret = new ListResult<>();
		ret.total = Math.max(count, items.size());
		ret.values = items;
		return ret;
	}

	public ListResult<Item> searchManageable(DirEntryQuery q, List<ManageableOrgUnit> manageable) throws SQLException {
		String fakeOu = "";
		Set<Kind> rootKinds = manageable.stream().filter(ou -> ou.ou == null).map(rootOU -> rootOU.kinds)
				.flatMap(Collection::stream).collect(Collectors.toSet());
		manageable.stream().filter(ou -> ou.ou != null).forEach(ou -> {
			Set<Kind> kinds = new HashSet<>(ou.kinds);
			kinds.removeAll(rootKinds);
			ou.kinds = kinds;
		});
		manageable = manageable.stream().filter(ou -> !ou.kinds.isEmpty()).collect(Collectors.toList());
		if (!manageable.stream().anyMatch(ou -> ou.ou != null)) {
			fakeOu = " (SELECT NULL::integer as id, NULL::text[] kinds) AS ";
		}
		unaccent(q);
		String ou = " RECURSIVE delegation(kind, id) AS ( " //
				+ "     SELECT kind, item_id" //
				+ "     FROM manageable" //
				+ "     JOIN t_container_item ON ou = uid" //
				+ "     JOIN t_directory_ou ON item_id = id "//
				+ "   UNION ALL" //
				+ "     SELECT kind, t_directory_ou.item_id" //
				+ "     FROM t_directory_ou " //
				+ "     JOIN delegation ON parent_item_id = delegation.id" //
				+ "), manageable AS (" //
				+ "    SELECT ou, json_array_elements_text(kinds) as kind, (ou IS NULL) as root_ou " //
				+ "    FROM json_to_recordset(?::json) as serialized_ou (kinds json, ou text) " //
				+ "), ou AS (" //
				+ "    SELECT id, array_agg(kind) as kinds FROM delegation GROUP BY id " //
				+ ")";

		String parameters = "(" //
				+ " SELECT (?::text[]) as kind," //
				+ " (?::t_directory_entry_account_type) as account_type," //
				+ " (?::text) as name," //
				+ " (?::text) as nameOrEmail, "//
				+ " (?::text) as nameOrEmailSplitted, "//
				+ " (?::text) as email," //
				+ " (?::text) as emailLeftPart," //
				+ " (?::text) as hidden, " //
				+ " (?::text) as system , "//
				+ " (?::text[]) as entryuid, " //
				+ " (?::boolean) as archived," //
				+ " (?::text[]) as root_kind, " //
				+ " (?::text) as datalocation " //
				+ ") AS parameters";

		String baseQuery = " FROM  t_container_item item " //
				+ " JOIN  t_directory_entry dir ON item.id = dir.item_id" //
				+ " LEFT JOIN " + fakeOu + " ou ON ou.id = dir.orgunit_item_id AND dir.kind = ANY(kinds), " //
				+ parameters //
				+ " WHERE item.container_id = ? " //
				+ " AND ( parameters.kind is null or dir.kind = ANY ( parameters.kind ))" //
				+ " AND ( parameters.account_type is null or dir.account_type = parameters.account_type )" //
				+ " AND ( parameters.name is null or unaccent(dir.displayname) ilike parameters.name) " //
				+ " AND ( " //
				+ " 	parameters.nameOrEmail is null " //
				+ "		or " //
				+ "		(dir.email ilike parameters.nameOrEmail or unaccent(dir.displayname) ilike parameters.nameOrEmail)" //
				+ ") " //
				+ " AND ( parameters.email is null or dir.email ilike parameters.email) " //
				+ " AND ( parameters.hidden is null or dir.flag_hidden = false )"
				+ " AND ( parameters.system is null or dir.flag_system = false )"
				+ " AND ( parameters.entryuid is null or item.uid = ANY ( parameters.entryuid ))" //
				+ " AND ( parameters.archived is null or dir.flag_archived = parameters.archived )" //
				+ " AND ( dir.kind = ANY(parameters.root_kind) OR ou.id IS NOT NULL ) "
				+ " AND ( parameters.datalocation is null or dir.datalocation = parameters.datalocation)";

		String countQuery = "WITH" + ou + " SELECT count(dir.item_id) " + baseQuery;

		String selectQuery = "WITH" + ou + " SELECT " + ItemStore.COLUMNS.names("item") + baseQuery + " ORDER BY ";

		if (q.order == null) {
			q.order = DirEntryQuery.defaultOrder();
		}

		switch (q.order.by) {
		case displayname:
			selectQuery += "dir.displayname";
			break;
		case kind:
		default:
			selectQuery += "dir.kind";
			break;
		}

		switch (q.order.dir) {
		case desc:
			selectQuery += " DESC";
			break;
		case asc:
		default:
			selectQuery += " ASC";
			break;
		}
		selectQuery += " LIMIT ? OFFSET ?";

		ArrayList<Object> params = new ArrayList<Object>();

		params.add(JsonUtils.asString(manageable));

		if (q.kindsFilter != null && q.kindsFilter.size() > 0) {
			String[] kinds = new String[q.kindsFilter.size()];

			int i = 0;
			for (Kind k : q.kindsFilter) {
				kinds[i] = k.name();
				i++;
			}
			params.add(kinds);

		} else {
			params.add(null);
		}

		if (q.accountTypeFilter != null) {
			params.add(q.accountTypeFilter.name());
		} else {
			params.add(null);
		}

		if (q.nameFilter != null) {
			params.add("%" + q.nameFilter + "%");
		} else {
			params.add(null);
		}

		if (q.nameOrEmailFilter != null) {
			String splitted = q.nameOrEmailFilter;
			if (splitted.indexOf('@') > 0) {
				splitted = splitted.substring(0, splitted.indexOf('@'));
			}

			params.add("%" + q.nameOrEmailFilter + "%");
			params.add("%" + splitted);
		} else {
			params.add(null);
			params.add(null);
		}

		if (q.emailFilter != null) {
			params.add(q.emailFilter);
			params.add(q.emailFilter.split("@")[0]);
		} else {
			params.add(null);
			params.add(null);
		}

		if (q.hiddenFilter) {
			params.add("filterHidden");
		} else {
			params.add(null);
		}

		if (q.systemFilter) {
			params.add("filterSystem");
		} else {
			params.add(null);
		}

		if (q.entryUidFilter != null && q.entryUidFilter.size() > 0) {
			String[] entryUids = new String[q.entryUidFilter.size()];

			int i = 0;
			for (String k : q.entryUidFilter) {
				entryUids[i] = k;
				i++;
			}
			params.add(entryUids);

		} else {
			params.add(null);
		}

		if (q.stateFilter != null && q.stateFilter != DirEntryQuery.StateFilter.All) {
			if (q.stateFilter == StateFilter.Active) {
				params.add(Boolean.FALSE);
			} else {
				params.add(Boolean.TRUE);
			}
		} else {
			params.add(null);
		}
		params.add(rootKinds.stream().map(Kind::toString).toArray(size -> new String[size]));

		if (!Strings.isNullOrEmpty(q.dataLocation)) {
			params.add(q.dataLocation);
		} else {
			params.add(null);
		}

		params.add(container.id);
		int count = -1;
		if (q.size <= 0) {
			selectQuery = selectQuery.replace("LIMIT ?", "");
		} else {
			count = unique(countQuery, new IntegerCreator(1), Collections.<EntityPopulator<Integer>>emptyList(),
					params.toArray());
			params.add(q.size);
		}

		params.add(q.from);

		List<Item> items = select(selectQuery, (rs) -> new Item(), Arrays.asList(ItemStore.ITEM_POPULATOR),
				params.toArray());

		ListResult<Item> ret = new ListResult<>();
		ret.total = Math.max(count, items.size());
		ret.values = items;
		return ret;
	}

	@Override
	public void delete(Item item) throws SQLException {
		delete("DELETE FROM t_directory_entry WHERE item_id = ?", new Object[] { item.id });
	}

	@Override
	public void deleteAll() throws SQLException {
		delete("DELETE FROM t_directory_entry WHERE item_id IN ( SELECT id FROM t_container_item WHERE  container_id = ?)",
				new Object[] { container.id });
	}

	public void updateAccountType(Item item, AccountType at) throws SQLException {
		update("update t_directory_entry set account_type = ?::t_directory_entry_account_type where item_id = ?",
				new Object[] { at.name(), item.id });
	}

	private void unaccent(DirEntryQuery query) {
		query.nameFilter = unaccent(query.nameFilter);
		query.nameOrEmailFilter = unaccent(query.nameOrEmailFilter);
		query.emailFilter = unaccent(query.emailFilter);
	}

	private String unaccent(String s) {
		if (s == null) {
			return null;
		}
		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		return s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
	}
}

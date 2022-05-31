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
package net.bluemind.core.container.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.i18n.labels.I18nLabels;

public class ContainerStore extends JdbcAbstractStore {

	// FIXME stores should not be aware of security context ?
	private SecurityContext securityContext;

	private ContainerCache cache;

	/**
	 * https://shipilev.net/jvm-anatomy-park/10-string-intern/
	 */
	public static class CHMInterner {
		private final Map<String, String> map;

		public CHMInterner() {
			map = new ConcurrentHashMap<>();
		}

		public String intern(String s) {
			if (s == null) {
				return s;
			}
			String exist = map.putIfAbsent(s, s);
			return (exist == null) ? s : exist;
		}
	}

	private static final CHMInterner interner = new CHMInterner();

	private static final EntityPopulator<Container> CONTAINER_POPULATOR = new EntityPopulator<Container>() {

		@Override
		public int populate(ResultSet rs, int index, Container value) throws SQLException {
			value.id = rs.getLong(index++);
			value.uid = rs.getString(index++);
			value.type = interner.intern(rs.getString(index++));
			value.name = rs.getString(index++);
			value.owner = rs.getString(index++);
			value.createdBy = rs.getString(index++);
			value.updatedBy = rs.getString(index++);

			value.created = new Date(rs.getTimestamp(index++).getTime());
			value.updated = new Date(rs.getTimestamp(index++).getTime());
			value.defaultContainer = rs.getBoolean(index++);
			value.domainUid = interner.intern(rs.getString(index++));
			value.readOnly = rs.getBoolean(index++);
			return index;
		}

	};

	/**
	 * please prefer the
	 * {@link ContainerStore#ContainerStore(BmContext, DataSource, SecurityContext)}
	 * variant that enables caching.
	 * 
	 * @param dataSource
	 * @param securityContext
	 */
	@Deprecated
	public ContainerStore(DataSource dataSource, SecurityContext securityContext) {
		this(null, dataSource, securityContext);
	}

	public ContainerStore(BmContext ctx, DataSource dataSource, SecurityContext securityContext) {
		super(dataSource);
		this.securityContext = securityContext;
		this.cache = ContainerCache.get(ctx, dataSource);
	}

	public List<Container> findByTypeAndOwner(String containerType, String owner) throws SQLException {
		return findByTypeOwnerReadOnly(containerType, owner, null);
	}

	public List<Container> findByTypeOwnerReadOnly(String containerType, String owner, Boolean readOnly)
			throws SQLException {
		String selectQuery = "SELECT id, uid, container_type, name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly FROM t_container AS c "
				+ " WHERE c.owner = ?";

		Object[] args = null;
		if (containerType != null && readOnly != null) {
			selectQuery += " AND c.container_type = ? AND c.readonly = ?";
			args = new Object[] { owner, containerType, readOnly };
		} else if (containerType != null) {
			selectQuery += " AND c.container_type = ? ";
			args = new Object[] { owner, containerType };
		} else {
			args = new Object[] { owner };
		}

		return select(selectQuery, rs -> new Container(),
				Arrays.<EntityPopulator<Container>>asList(CONTAINER_POPULATOR), args);

	}

	public List<Container> findByType(String containerType) throws SQLException {
		String selectQuery = "SELECT id, uid, container_type, name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly FROM t_container AS c "
				+ " WHERE c.container_type = ?";

		return select(selectQuery, (rs) -> new Container(),
				Arrays.<EntityPopulator<Container>>asList(CONTAINER_POPULATOR), new Object[] { containerType });

	}

	public List<Container> findAccessiblesByType(final ContainerQuery query) throws SQLException {
		if (null != query.name) {
			Set<Container> containers = new HashSet<>();
			List<String> names = I18nLabels.getInstance().getMatchingKeys(query.name, securityContext.getLang());
			names.add(query.name);
			for (String queryName : names) {
				query.name = queryName;
				containers.addAll(findAccessiblesByTypeImpl(query));
				if (query.size > 0 && containers.size() >= query.size) {
					break;
				}
			}
			return new ArrayList<>(containers);
		} else {
			return findAccessiblesByTypeImpl(query);
		}
	}

	private List<Container> findAccessiblesByTypeImpl(final ContainerQuery query) throws SQLException {
		// FIXME we need to restrict to domain_uid
		StringBuilder q = new StringBuilder();
		q.append(
				"SELECT id, uid, container_type, name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly ");
		q.append(" FROM t_container AS c");
		q.append(" WHERE (c.owner = ? ");
		q.append(
				"OR c.id IN (SELECT acl.container_id FROM t_container_acl AS acl WHERE (acl.subject = ANY(?) OR acl.subject = 'public') ");
		if (query.verb != null && !query.verb.isEmpty()) {
			q.append(" AND acl.verb = ANY(?)");
		}
		q.append("))");

		String subject = securityContext.getSubject();
		List<String> memberOf = securityContext.getMemberOf();
		List<String> subjects = new ArrayList<>(memberOf.size() + 1);
		subjects.addAll(memberOf);
		subjects.add(subject);

		// public share
		subjects.add(securityContext.getContainerUid());

		String[] s = subjects.toArray(new String[subjects.size()]);
		List<Object> parameters = new LinkedList<>();
		parameters.add(subject);
		parameters.add(s);

		if (query.verb != null && !query.verb.isEmpty()) {
			List<String> verbs = new ArrayList<>();
			for (Verb v : query.verb) {
				verbs.add(v.name());
			}
			parameters.add(verbs.toArray(new String[verbs.size()]));
		}

		if (query.type != null) {
			q.append(" AND c.container_type = ?");
			parameters.add(query.type);
		}

		if (query.readonly != null) {
			q.append(" AND c.readonly = ?");
			parameters.add(query.readonly);
		}

		if (query.name != null) {
			if (query.name.startsWith("$$")) {
				q.append(" AND name = ?");
				parameters.add(query.name);
			} else {
				q.append(" AND (upper(name) like upper(?) AND name NOT LIKE '$$%')");
				parameters.add("%" + query.name + "%");
			}
		}

		q.append(" ORDER BY upper(name) ASC");

		if (query.size > 0) {
			q.append(" LIMIT ? ");
			parameters.add(query.size);
		}

		return select(q.toString(), rs -> new Container(),
				Arrays.<EntityPopulator<Container>>asList(CONTAINER_POPULATOR), parameters.toArray());

	}

	public Container create(Container container) throws SQLException {

		String insertQuery = "INSERT INTO t_container (uid, container_type, "
				+ "name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly) "
				+ " VALUES (?, ?, ?, ?, ?, ?, now(), now(), ?, ?, ?)";
		insert(insertQuery, container,
				Arrays.<StatementValues<Container>>asList((con, statement, index, rowIndex, value) -> {

					statement.setString(index++, value.uid);
					statement.setString(index++, value.type);
					statement.setString(index++, value.name);
					statement.setString(index++, value.owner);
					String principal = securityContext.getSubject();
					statement.setString(index++, principal);
					statement.setString(index++, principal);
					statement.setBoolean(index++, value.defaultContainer);
					statement.setString(index++, value.domainUid);
					statement.setBoolean(index++, value.readOnly);
					return index;
				}));

		Container c = get(container.uid);

		// container settings
		insert("INSERT INTO t_container_settings (container_id, settings) VALUES (?, '')", new Object[] { c.id });
		// insert seq
		insert("INSERT INTO t_container_sequence (container_id) VALUES (?)", new Object[] { c.id });
		return c;
	}

	public void updateName(final String uid, final String name) throws SQLException {
		String updateQuery = "UPDATE t_container SET (name, updatedby, updated) = (?, ?, now()) WHERE uid = ?";
		update(updateQuery, null, (con, statement, index, currentRow, value) -> {
			statement.setString(index++, name);
			statement.setString(index++, securityContext.getSubject());
			statement.setString(index++, uid);
			return index;
		});
		invalidateCache(uid, get(uid).id);
	}

	public Container get(String uid) throws SQLException {
		Container c = cache.getIfPresent(uid);
		if (c == null) {
			String selectQuery = "SELECT id, uid, container_type, name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly FROM t_container WHERE uid = ?";

			c = unique(selectQuery, rs -> new Container(),
					Arrays.<EntityPopulator<Container>>asList(CONTAINER_POPULATOR), new Object[] { uid });
			if (c != null) {
				cache.put(uid, c.id, c);
				return c.copy();
			} else {
				return null;
			}
		} else {
			return c.copy();
		}
	}

	public Container get(long id) throws SQLException {
		Container c = cache.getIfPresent(id);
		if (c == null) {
			String selectQuery = "SELECT id, uid, container_type, name, owner, createdby, updatedby, created, updated, defaultContainer, domain_uid, readonly FROM t_container WHERE id = ?";

			c = unique(selectQuery, rs -> new Container(),
					Arrays.<EntityPopulator<Container>>asList(CONTAINER_POPULATOR), new Object[] { id });
			if (c != null) {
				cache.put(c.uid, c.id, c);
				return c.copy();
			} else {
				return null;
			}
		} else {
			return c.copy();
		}

	}

	public void deleteAllSubscriptions(Container container) throws SQLException {
		delete("DELETE FROM t_container_sub WHERE container_uid = ? ", new Object[] { container.uid });
	}

	public void delete(String uid) throws SQLException {
		Container c = get(uid);
		if (c == null) {
			throw ServerFault.notFound(uid);
		}
		deleteKnownIdUid(c.id, uid);
	}

	public void deleteKnownIdUid(long id, String uid) throws SQLException {
		delete("DELETE FROM t_container_settings WHERE container_id = ?", new Object[] { id });
		delete("DELETE FROM t_container_sequence WHERE container_id = ?", new Object[] { id });
		delete("DELETE FROM t_container_item WHERE container_id = ?", new Object[] { id });
		delete("DELETE FROM t_container WHERE id = ?", new Object[] { id });
		invalidateCache(uid, id);
	}

	public void invalidateCache(String uid, Long id) {
		cache.invalidate(uid, id);
	}

	public List<String> listSubscriptions(Container container) throws SQLException {
		return select(
				"SELECT uid FROM t_container_sub INNER JOIN t_container_item ON id=user_id WHERE t_container_sub.container_uid = ?",
				StringCreator.FIRST, Collections.emptyList(), new Object[] { container.uid });

	}

	/**
	 * Creates or updates given container's location
	 * 
	 * @param container
	 * @param location
	 * @throws SQLException
	 */
	public void createContainerLocation(Container container, String location) throws SQLException {
		insert("INSERT INTO t_container_location VALUES (?, ?) ON CONFLICT (container_uid) DO UPDATE SET location = ? WHERE t_container_location.container_uid = ?",
				container, (con, statement, index, currentRow, value) -> {

					statement.setString(index++, value.uid);
					statement.setString(index++, location);
					statement.setString(index++, location);
					statement.setString(index++, value.uid);

					return index;
				});
	}

	public void deleteContainerLocation(Container container) throws SQLException {
		delete("DELETE FROM t_container_location WHERE container_uid = ?", new Object[] { container.uid });
	}

	/**
	 * Returns null if the container location is unknown, or an optional if the
	 * location is known.
	 * 
	 * @param containerUid
	 * @return
	 * @throws SQLException
	 */
	public Optional<String> getContainerLocation(String containerUid) throws SQLException {
		String ret = unique("SELECT coalesce(location, 'DIR') FROM t_container_location WHERE container_uid = ?",
				StringCreator.FIRST, Collections.emptyList(), new Object[] { containerUid });
		if (ret == null) {
			return null;
		} else if ("DIR".equals(ret)) {
			return Optional.empty();
		} else {
			return Optional.of(ret);
		}
	}

	/**
	 * Returns all container uids belonging to a different server
	 * 
	 * @param serverUid
	 * @return
	 * @throws SQLException
	 */
	private List<String> getForeignContainers(String location) throws SQLException {
		if (location == null) {
			return select(
					"SELECT container_uid FROM t_container_location WHERE location IS NOT NULL AND location <> ''",
					StringCreator.FIRST, Collections.emptyList(), new Object[0]);
		} else {
			return select(
					"SELECT c.uid FROM t_container c LEFT JOIN t_container_location cl ON c.uid = cl.container_uid WHERE cl.location IS NULL OR cl.location = '' OR cl.location <> ?",
					StringCreator.FIRST, Collections.emptyList(), new Object[] { location });
		}
	}

	public Set<String> getObsoleteContainers(String location) throws SQLException {
		List<String> containers = select("SELECT c.uid FROM t_container c WHERE c.container_type = ?",
				StringCreator.FIRST, Collections.emptyList(), new Object[] { "t_folder" });
		containers.addAll(getForeignContainers(location));
		return new HashSet<>(containers);
	}

	public Set<String> getMissingContainerSequence() throws SQLException {
		String query = "select uid from t_container where id not in (select container_id from t_container_sequence)";
		List<String> containers = select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] {});
		return new HashSet<>(containers);
	}

	public Set<String> getMissingContainerSettings() throws SQLException {
		String query = "select uid from t_container where id not in (select container_id from t_container_sequence)";
		List<String> containers = select(query, StringCreator.FIRST, Collections.emptyList(), new Object[] {});
		return new HashSet<>(containers);
	}

	public void createMissingContainerSequence() throws SQLException {
		insert("insert into t_container_sequence select container_id, max(version) from t_container_item where container_id not in (select container_id from t_container_sequence) GROUP BY container_id",
				new Object[] {});
		insert("insert into t_container_sequence select id, 0 from t_container where id not in (select container_id from t_container_sequence)",
				new Object[] {});
	}

	public void createMissingContainerSettings() throws SQLException {
		insert("insert into t_container_settings select id, '' from t_container where id not in(select container_id from t_container_settings)",
				new Object[] {});
	}

}

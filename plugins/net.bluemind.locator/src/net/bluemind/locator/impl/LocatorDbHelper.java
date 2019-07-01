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
package net.bluemind.locator.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.pool.BMPoolActivator;
import net.bluemind.pool.impl.BmConfIni;

public class LocatorDbHelper {

	private static final Logger logger = LoggerFactory.getLogger(LocatorDbHelper.class);

	/**
	 * For those services, we fetch a per-user data location.
	 */
	private static final Set<String> dataLocationBasedServices = Sets.newHashSet("mail/imap");

	public LocatorDbHelper() {
	}

	/**
	 * Returns all hosts assigned to the user's domain with the given tag if the
	 * service is not per-user. For per-user services, the user's data location is
	 * used.
	 * 
	 * @param latdOrEmail login@domain or any email a user
	 * @param tag
	 * @return
	 */
	public static Set<String> findUserAssignedHosts(String latdOrEmail, String tag) {
		int idx = latdOrEmail.indexOf('@');
		String domain = "global.virt";
		String login = "";
		if (idx > 0) {
			domain = latdOrEmail.substring(idx + 1);
			login = latdOrEmail.substring(0, idx);
		}
		boolean global = "global.virt".equals(domain);

		Set<String> allAssigned = findAssignedHosts(domain, global, tag);
		if (!global && allAssigned.size() > 1 && dataLocationBasedServices.contains(tag)) {
			logger.info("Going to fetch user data loc");
			String userDataLocation = getUserDataLocation(login, domain);
			return userDataLocation != null ? Sets.newHashSet(userDataLocation) : Collections.emptySet();
		} else {
			return allAssigned;
		}
	}

	/**
	 * Returns the IP addresses of the hosts with the given tag assigned to a
	 * domain.
	 * 
	 * @param domain domain uid
	 * @param global true if the domain is global
	 * @param tag    the server tag (eg. mail/imap_frontend)
	 * 
	 * @return a unique ip or fdqn assigned to the domain
	 */
	public static Set<String> findAssignedHosts(String domain, boolean global, String tag) {

		StringBuilder find = new StringBuilder();

		if (!global) {
			find.append(" SELECT");
			find.append(" coalesce(s.ip, s.fqdn)");
			find.append(" FROM t_server_assignment sa");
			find.append(" INNER JOIN t_container_item ci ON ci.uid=sa.server_uid");
			find.append(" INNER JOIN t_server s ON ci.id=s.item_id");
			find.append(" INNER JOIN t_domain d ON d.name = sa.domain_uid");
			find.append(" WHERE ?=sa.tag");
			find.append(" AND (sa.domain_uid=? OR ? = ANY(d.aliases))");
		} else {
			find.append(" SELECT");
			find.append(" coalesce(s.ip, s.fqdn)");
			find.append(" FROM t_server s");
			find.append(" INNER JOIN t_container_item ci ON ci.id=s.item_id");
			find.append(" WHERE ?=ANY(s.tags)");
		}

		Set<String> ret = new HashSet<String>();
		DataSource ds = JdbcActivator.getInstance().getDataSource();
		if (ds == null) {
			logger.error("database is not avalaible");
			// database is not available
			return ret;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = ds.getConnection();
			ps = con.prepareStatement(find.toString());
			ps.setString(1, tag);
			if (!global) {
				ps.setString(2, domain);
				ps.setString(3, domain);
			}

			rs = ps.executeQuery();
			while (rs.next()) {
				String addr = rs.getString(1);
				ret.add(addr);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			if (global && "bm/core".equals(tag)) {
				// database is not ready, use bm.ini to resolve bm/core for
				// admin0@global.virt
				String host = new BmConfIni().get("host");
				logger.info("use bm.ini to resolve bm/core");
				ret.add(host);
			}
		} finally {
			BMPoolActivator.cleanup(con, ps, rs);
		}

		return ret;
	}

	/**
	 * Returns a user's datalocation (ip or fdqn, not server uid) if one of its
	 * email matches the given local part & domain part.
	 * 
	 * @param localpart
	 * @param domainpart
	 * 
	 * @return ip or fqdn to use
	 */
	private static String getUserDataLocation(String localpart, String domainpart) {

		// inspired by DirItemStore#byEmail
		String query = "WITH domain_uid AS " + //
				"( select name from t_domain where name=? or ?=ANY(aliases) ), " + //
				"domain_dir AS " + //
				"( " + //
				"   SELECT " + //
				"   id " + //
				"   FROM t_container cont, domain_uid " + //
				"   where container_type='dir' " + //
				"   AND cont.uid=domain_uid.name " + //
				") " + //
				"SELECT " + //
				"coalesce(srv.ip, srv.fqdn) " + //
				"FROM domain_dir, " + //
				"t_container_item item, " + //
				"t_directory_entry dir, " + //
				"t_container_item sitem, " + //
				"t_server srv " + //
				"WHERE item.container_id=domain_dir.id " + //
				"AND dir.item_id = item.id " + //
				"AND srv.item_id=sitem.id " + //
				"AND dir.datalocation=sitem.uid " + //
				"AND exists " + //
				"( " + //
				"   select " + //
				"   * " + //
				"   from t_mailbox_email e " + //
				"   where e.item_id = item.id " + //
				"   and " + //
				"   ( " + //
				"      ( " + //
				"         e.left_address=? " + //
				"         AND e.right_address = ? " + //
				"      ) " + //
				"      or ( e.all_aliases = true and e.left_address = ?) " + //
				"   ) " + //
				") ";
		String ret = null;
		DataSource ds = JdbcActivator.getInstance().getDataSource();
		if (ds == null) {
			logger.error("database is not avalaible");
			return ret;
		}

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = ds.getConnection();
			ps = con.prepareStatement(query);
			int i = 1;
			ps.setString(i++, domainpart);
			ps.setString(i++, domainpart);
			ps.setString(i++, localpart);
			ps.setString(i++, domainpart);
			ps.setString(i++, localpart);

			logger.debug("ps: {}", ps);

			rs = ps.executeQuery();
			if (rs.next()) {
				ret = rs.getString(1);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			BMPoolActivator.cleanup(con, ps, rs);
		}

		return ret;
	}
}

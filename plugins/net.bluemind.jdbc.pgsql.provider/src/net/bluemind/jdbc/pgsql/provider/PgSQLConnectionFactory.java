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
package net.bluemind.jdbc.pgsql.provider;

import net.bluemind.pool.IJDBCDriver;

public class PgSQLConnectionFactory implements IJDBCDriver {

	@Override
	public String getSupportedDbType() {
		return "pgsql";
	}

	@Override
	public String getDriverClass() {
		return "org.postgresql.Driver";
	}

	@Override
	public String getJDBCUrl(String host, String dbName, String login, String password) {
		return "jdbc:postgresql://" + host + "/" + dbName;
	}

	@Override
	public String getKeepAliveQuery() {
		return "SELECT 1";
	}

	@Override
	public String getLastInsertIdQuery() {
		return "SELECT lastval()";
	}

}

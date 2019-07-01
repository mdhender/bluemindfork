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
package net.bluemind.pool;

public interface IJDBCDriver {

	/**
	 * Returns the supported dbtype as in bm.ini
	 * 
	 * @return mysql or pgsql
	 */
	String getSupportedDbType();

	/**
	 * @return the jdbc driver that should be used with the given db type
	 */
	String getDriverClass();

	/**
	 * @return returns an SQL query that always work when the sql connection
	 *         works
	 */
	String getKeepAliveQuery();

	String getJDBCUrl(String host, String dbName, String login, String password);

	String getLastInsertIdQuery();

}

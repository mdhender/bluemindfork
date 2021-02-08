/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.maintenance.tests;

import java.io.IOException;
import java.io.InputStream;

import net.bluemind.system.pg.PostgreSQLService;

public class TestPostgreSQLService extends PostgreSQLService {
	@Override
	protected InputStream getCreateDbScript() throws IOException {
		return this.getClass().getClassLoader().getResourceAsStream("data/install_bmdb_pgsql_0.sh");
	}

	@Override
	protected InputStream getConf(String conf) throws IOException {
		return this.getClass().getClassLoader().getResourceAsStream("data/" + conf);
	}
}

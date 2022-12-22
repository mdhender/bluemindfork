/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.persistence;

import org.junit.After;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class AbstractStoreTests {

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected SmimeCacert defaultSmimeCacert() {
		SmimeCacert certif = new SmimeCacert();
		certif.cert = "SmimeCacert " + System.currentTimeMillis();
		return certif;
	}
}

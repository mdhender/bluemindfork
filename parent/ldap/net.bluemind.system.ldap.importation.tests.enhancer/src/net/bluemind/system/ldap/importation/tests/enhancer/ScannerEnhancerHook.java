/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.importation.tests.enhancer;

import static org.junit.Assert.assertNotNull;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.enhancer.IScannerEnhancer;
import net.bluemind.system.importation.commons.scanner.IImportLogger;

public class ScannerEnhancerHook implements IScannerEnhancer {
	public static boolean before;
	public static boolean after;

	@Override
	public void beforeImport(IImportLogger importLogger, Parameters parameter, ItemValue<Domain> domain,
			LdapConProxy ldapCon) {
		assertNotNull(importLogger);
		assertNotNull(parameter);
		assertNotNull(domain);
		assertNotNull(ldapCon);
		before = true;
	}

	@Override
	public void afterImport(IImportLogger importLogger, Parameters parameter, ItemValue<Domain> domain,
			LdapConProxy ldapCon) {
		assertNotNull(importLogger);
		assertNotNull(parameter);
		assertNotNull(domain);
		assertNotNull(ldapCon);
		after = true;
	}

	public static void initFlags() {
		before = after = false;
	}
}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.importation.commons.tools;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.importation.commons.Parameters;

public abstract class DirectoryTester {
	private Parameters parameters;

	protected DirectoryTester(Parameters parameters) {
		this.parameters = parameters;
	}

	protected abstract LdapConnection getDirectoryConnection(Parameters parameters);

	public void testDirectoryParameters() throws ServerFault {
		try (LdapConnection ldapCon = getDirectoryConnection(parameters)) {
			// Check if base DN is found
			checkBaseDn(parameters.ldapDirectory.baseDn, ldapCon);
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e.getMessage());
		}
	}

	private void checkBaseDn(Dn baseDn, LdapConnection ldapCon) throws Exception {
		SearchRequestImpl searchRequest = new SearchRequestImpl();
		searchRequest.setScope(SearchScope.OBJECT);
		searchRequest.setBase(baseDn);
		searchRequest.setFilter("(objectclass=*)");
		SearchCursor result = ldapCon.search(searchRequest);

		try {
			if (!result.next()) {
				throw new ServerFault("Base DN not found, check import parameter or set server default search base",
						ErrorCode.NOT_FOUND);
			}
		} finally {
			result.close();
		}
	}
}

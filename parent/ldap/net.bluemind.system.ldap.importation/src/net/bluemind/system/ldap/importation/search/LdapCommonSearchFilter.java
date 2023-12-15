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
package net.bluemind.system.ldap.importation.search;

import java.util.Optional;

import net.bluemind.core.api.Regex;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.search.SearchFilter;
import net.bluemind.system.ldap.importation.api.LdapConstants;

public abstract class LdapCommonSearchFilter implements SearchFilter {
	protected abstract String nameCondition(String name);

	protected abstract <T extends Parameters> String getFilter(T ldapParameters);

	@Override
	public <T extends Parameters> String getSearchFilter(T ldapParameters) {
		return getSearchFilter(ldapParameters, Optional.empty(), Optional.empty(), Optional.empty());
	}

	@Override
	public <T extends Parameters> String getSearchFilterByLastModification(T ldapParameters,
			Optional<String> lastUpdate) {
		return getSearchFilter(ldapParameters, lastUpdate, Optional.empty(), Optional.empty());
	}

	@Override
	public <T extends Parameters> String getSearchFilterByUuid(T ldapParameters, String uuid) {
		return getSearchFilter(ldapParameters, Optional.empty(), Optional.of(uuid), Optional.empty());
	}

	@Override
	public <T extends Parameters> String getSearchFilterByName(T ldapParameters, String name) {
		return getSearchFilter(ldapParameters, Optional.empty(), Optional.empty(), Optional.of(name));
	}

	private <T extends Parameters> String getSearchFilter(T ldapParameters, Optional<String> lastUpdate,
			Optional<String> uuid, Optional<String> name) {
		String filter = getFilter(ldapParameters);
		String conditions = "";

		conditions += lastUpdate.map(this::lastModificationCondition).orElse("");
		conditions += uuid.map(u -> uuidCondition(ldapParameters.ldapDirectory.extIdAttribute, u)).orElse("");
		conditions += name.map(this::nameCondition).orElse("");

		if (!"".equals(conditions)) {
			filter = "(&" + filter + conditions + ")";
		}

		return filter;
	}

	protected String lastModificationCondition(String lastUpdate) {
		if (lastUpdate == null || lastUpdate.isBlank()) {
			return null;
		}

		return "(" + LdapConstants.MODIFYTIMESTAMP_ATTR + ">=" + lastUpdate + ")";
	}

	protected String uuidCondition(String uuidAttribute, String uuid) {
		if (uuidAttribute == null || uuidAttribute.isBlank()) {
			return null;
		}

		if (!Regex.UUID.validate(uuid)) {
			return null;
		}

		return "(" + uuidAttribute + "=" + uuid + ")";
	}
}

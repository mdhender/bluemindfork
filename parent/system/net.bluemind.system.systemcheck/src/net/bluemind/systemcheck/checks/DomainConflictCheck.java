/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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

package net.bluemind.systemcheck.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.InstallationVersion;

public class DomainConflictCheck extends AbstractCheck {
	private static final String CHECK_KEY = "check.domainConflictCheck";

	@Override
	public boolean canCheckWithVersion(InstallationVersion version) {
		return version.databaseVersion.startsWith("4.");
	}

	@Override
	public CheckResult verify(IServiceProvider provider, SetupCheckResults results, Map<String, String> collected)
			throws Exception {
		try {
			IDomains domains = provider.instance(IDomains.class);
			List<ItemValue<Domain>> allDomains = domains.all();
			List<String> allNames = new ArrayList<>();
			for (ItemValue<Domain> ivdomain : allDomains) {
				Domain domain = ivdomain.value;
				if (allNames.contains(domain.defaultAlias)) {
					return cr(CheckState.ERROR, CHECK_KEY, "Domain uid:" + domain.name + " defaultAlias: "
							+ domain.defaultAlias + " is conflicting with another domain");
				}
				for (String alias : domain.aliases) {
					if (allNames.contains(alias)) {
						return cr(CheckState.ERROR, CHECK_KEY, "Domain uid:" + domain.name + " alias: " + alias
								+ " is conflicting with another domain or alias");
					}
				}
				allNames.add(domain.defaultAlias);
				allNames.addAll(domain.aliases);
			}
		} catch (Exception e) {
			return cr(CheckState.OK, CHECK_KEY, "Skipping check: " + e.getMessage());
		}
		return cr(CheckState.OK, CHECK_KEY, "No domain name conflict detected.");
	}
}

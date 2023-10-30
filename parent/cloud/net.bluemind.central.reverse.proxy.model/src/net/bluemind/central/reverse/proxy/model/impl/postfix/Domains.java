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
package net.bluemind.central.reverse.proxy.model.impl.postfix;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Domains {
	private record Domain(DomainAliases domainAliases, DomainSettings domainSettings) {
	}

	public record DomainAliases(String uid, Collection<String> aliases) {
		public DomainAliases aliasOnly() {
			return new DomainAliases(null, aliases);
		}

		public boolean match(String alias) {
			return alias.equals(uid) || aliases.contains(alias);
		}
	}

	public record DomainSettings(String uid, String mailRoutingRelay, boolean mailForwardUnknown) {
	}

	private Map<String, Domain> domains = new HashMap<>();

	public void updateDomainAliases(String domainUid, Set<String> aliases) {
		DomainAliases domainAliases = new DomainAliases(domainUid, aliases);

		domains.compute(domainUid,
				(k, v) -> v == null ? new Domain(domainAliases, null) : new Domain(domainAliases, v.domainSettings));
	}

	public void updateDomainSetting(String domainUid, String mailRoutingRelay, boolean mailForwardUnknown) {
		DomainSettings domainSettings = new DomainSettings(domainUid, mailRoutingRelay, mailForwardUnknown);

		domains.compute(domainUid,
				(k, v) -> v == null ? new Domain(null, domainSettings) : new Domain(v.domainAliases, domainSettings));
	}

	public DomainAliases getDomainAliases(String domainUid) {
		Domain domain = domains.get(domainUid);
		if (domain == null) {
			return null;
		}

		return domain.domainAliases;
	}

	public DomainSettings getDomainSettings(String domainUid) {
		Domain domain = domains.get(domainUid);
		if (domain == null) {
			return null;
		}

		return domain.domainSettings;
	}

	public Optional<String> domainUidFromAlias(String alias) {
		return domains.values().stream().map(domain -> domain.domainAliases).filter(da -> da.match(alias)).findAny()
				.map(DomainAliases::uid);
	}

	public void removeDomain(String domainUid) {
		domains.remove(domainUid);
	}
}

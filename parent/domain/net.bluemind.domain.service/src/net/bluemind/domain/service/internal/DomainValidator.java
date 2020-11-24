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
package net.bluemind.domain.service.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class DomainValidator {

	public void validate(DomainStoreService store, Domain domain) throws ServerFault {
		ParametersValidator.notNull(domain);
		ParametersValidator.notNull(domain.aliases);
		ParametersValidator.notNullAndNotEmpty(domain.label);
		ParametersValidator.notNullAndNotEmpty(domain.name);
		boolean validDN = Regex.EMAIL.validate("fake@" + domain.name);
		if (!validDN) {
			throw new ServerFault(
					"" + domain.name + " is an invalid domain name, must be usable as the @<name> of an email",
					ErrorCode.INVALID_DOMAIN_NAME);
		}

		checkAliasesFormat(domain.aliases);
		checkAliasUniquity(store, domain.name, domain.aliases);
		checkDefaultAlias(domain);
	}

	private void checkDefaultAlias(Domain domain) throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(domain.defaultAlias);
		if (!domain.aliases.contains(domain.defaultAlias) && !domain.defaultAlias.equals(domain.name)) {
			throw new ServerFault("defaultAlias is neither equals to domain name or contained in domain aliases",
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private void checkAliasUniquity(DomainStoreService store, String name, Set<String> aliases) throws ServerFault {
		Set<String> checkset = new HashSet<>(aliases);
		checkset.add(name);

		Map<String, ItemValue<Domain>> presentAliases = checkset.stream() //
				.map(alias -> {
					ItemValue<Domain> domain = domainIsPresent(store, name, alias);
					if (domain != null && !domain.uid.equals(name)) {
						return (Map<String, ItemValue<Domain>>) ImmutableMap.<String, ItemValue<Domain>>builder()
								.put(alias, domain).build();
					} else {
						return (Map<String, ItemValue<Domain>>) ImmutableMap.<String, ItemValue<Domain>>of();
					}
				}).reduce(new HashMap<>(), (u, t) -> {
					u.putAll(t);
					return u;
				});

		if (!presentAliases.isEmpty()) {
			List<String> msg = presentAliases.entrySet().stream().map(e -> {
				return "alias " + e.getKey() + " conflict with domain: " + e.getValue().value.name;
			}).collect(Collectors.toList());
			throw new ServerFault(String.join(", ", msg), ErrorCode.INVALID_DOMAIN_NAME);
		}
	}

	private ItemValue<Domain> domainIsPresent(final DomainStoreService store, final String name, final String alias) {
		ItemValue<Domain> domainItem = null;
		try {
			domainItem = store.findByNameOrAliases(alias);
		} catch (Exception e) {
		}

		return domainItem;
	}

	private void checkAliasesFormat(Set<String> aliases) throws ServerFault {
		if (aliases != null) {
			for (String a : aliases) {
				String em = "fake@" + a;
				if (!Regex.EMAIL.validate(em)) {
					throw new ServerFault("'" + a + "' is an invalid alias name", ErrorCode.INVALID_DOMAIN_NAME);
				}
			}
		}
	}
}

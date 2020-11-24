/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import java.util.HashSet;
import java.util.Optional;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.domain.api.Domain;

public class DomainSanitizer implements ISanitizer<Domain> {

	public static class Factory implements ISanitizerFactory<Domain> {
		@Override
		public Class<Domain> support() {
			return Domain.class;
		}

		@Override
		public ISanitizer<Domain> create(BmContext context) {
			return new DomainSanitizer();
		}
	}

	@Override
	public void create(Domain domain) {
		setMissingAlias(domain);
		setMissingDefaultAlias(domain);
	}

	@Override
	public void update(Domain current, Domain updated) {
		setMissingAlias(updated);
		setMissingDefaultAlias(updated);
	}

	private void setMissingAlias(Domain domain) {
		// For unit testing, we are not using the .internal domain uid
		// but we want a default alias to match domain name.
		if (!domain.name.endsWith(".internal") && !domain.aliases.contains(domain.name)) {
			domain.aliases = new HashSet<String>(domain.aliases);
			domain.aliases.add(domain.name);
		}
	}

	private void setMissingDefaultAlias(Domain domain) {
		if (domain.defaultAlias == null || domain.defaultAlias.isEmpty()) {
			Optional<String> defaultAlias = domain.aliases.stream().findFirst();
			if (defaultAlias.isPresent()) {
				domain.defaultAlias = defaultAlias.get();
			}
		}
	}
}

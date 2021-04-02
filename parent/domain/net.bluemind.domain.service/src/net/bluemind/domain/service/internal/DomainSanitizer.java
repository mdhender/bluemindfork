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

import net.bluemind.core.container.model.Container;
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
		public ISanitizer<Domain> create(BmContext context, Container container) {
			return new DomainSanitizer();
		}
	}

	@Override
	public void create(Domain domain) {
		setMissingDefaultAlias(domain);
	}

	@Override
	public void update(Domain current, Domain updated) {
		setMissingDefaultAlias(updated);
	}

	private void setMissingDefaultAlias(Domain domain) {
		if (domain.defaultAlias == null || domain.defaultAlias.isEmpty()) {
			domain.defaultAlias = domain.aliases.stream().findFirst().orElse(domain.name);
		}
	}
}

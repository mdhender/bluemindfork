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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.smime.cacerts.service.internal;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertSanitizer implements ISanitizer<SmimeCacert> {

	public static class Factory implements ISanitizerFactory<SmimeCacert> {

		@Override
		public Class<SmimeCacert> support() {
			return SmimeCacert.class;
		}

		@Override
		public ISanitizer<SmimeCacert> create(BmContext context, Container container) {
			return new SmimeCacertSanitizer();
		}

	}

	private void sanitize(SmimeCacert cert) {
		if (null == cert) {
			return;
		}
	}

	@Override
	public void create(SmimeCacert obj) {
		sanitize(obj);
	}

	@Override
	public void update(SmimeCacert current, SmimeCacert obj) {
		sanitize(obj);
	}

}

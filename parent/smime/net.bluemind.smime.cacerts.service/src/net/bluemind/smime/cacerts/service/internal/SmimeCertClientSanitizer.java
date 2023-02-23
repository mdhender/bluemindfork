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
import net.bluemind.smime.cacerts.api.SmimeCertClient;

public class SmimeCertClientSanitizer implements ISanitizer<SmimeCertClient> {

	public static class Factory implements ISanitizerFactory<SmimeCertClient> {

		@Override
		public Class<SmimeCertClient> support() {
			return SmimeCertClient.class;
		}

		@Override
		public ISanitizer<SmimeCertClient> create(BmContext context, Container container) {
			return new SmimeCertClientSanitizer();
		}

	}

	private void sanitize(SmimeCertClient cert) {
		if (cert == null) {
			return;
		}

		if (cert.serialNumber != null) {
			cert.serialNumber = cert.serialNumber.toUpperCase();
		}
	}

	@Override
	public void create(SmimeCertClient obj) {
		sanitize(obj);
	}

	@Override
	public void update(SmimeCertClient current, SmimeCertClient obj) {
		sanitize(obj);
	}

}

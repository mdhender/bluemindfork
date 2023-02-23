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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.smime.cacerts.api.SmimeCertClient;

public class SmimeCertClientValidator implements IValidator<SmimeCertClient> {
	private static final Logger logger = LoggerFactory.getLogger(SmimeCertClientValidator.class);

	public static class Factory implements IValidatorFactory<SmimeCertClient> {

		@Override
		public Class<SmimeCertClient> support() {
			return SmimeCertClient.class;
		}

		@Override
		public IValidator<SmimeCertClient> create(BmContext context) {
			return new SmimeCertClientValidator();
		}

	}

	private void validate(SmimeCertClient cert) throws ServerFault {

		if (cert == null) {
			throw new ServerFault("SmimeCertClient is null", ErrorCode.INVALID_PARAMETER);
		}

		if (Strings.isNullOrEmpty(cert.serialNumber)) {
			throw new ServerFault("Serial number is null", ErrorCode.INVALID_PARAMETER);
		}

		if (Strings.isNullOrEmpty(cert.issuer)) {
			throw new ServerFault("Issuer is null", ErrorCode.INVALID_PARAMETER);
		}
	}

	@Override
	public void create(SmimeCertClient obj) {
		validate(obj);
	}

	@Override
	public void update(SmimeCertClient oldValue, SmimeCertClient newValue) {
		validate(newValue);
	}
}

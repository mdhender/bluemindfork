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

import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidator;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.utils.CertificateUtils;

public class SmimeCacertValidator implements IValidator<SmimeCacert> {
	private static final Logger logger = LoggerFactory.getLogger(SmimeCacertValidator.class);

	public static class Factory implements IValidatorFactory<SmimeCacert> {

		@Override
		public Class<SmimeCacert> support() {
			return SmimeCacert.class;
		}

		@Override
		public IValidator<SmimeCacert> create(BmContext context) {
			return new SmimeCacertValidator();
		}

	}

	private void validate(SmimeCacert cert) throws ServerFault {

		if (cert == null) {
			throw new ServerFault("SmimeCacert is null", ErrorCode.INVALID_PARAMETER);
		}

		if (cert.cert == null) {
			throw new ServerFault("S/MIME certificate is null", ErrorCode.INVALID_PARAMETER);
		}
		checkCertificate(cert.cert);
	}

	private void checkCertificate(String cert) {
		byte[] caData = cert.getBytes();

		try {
			CertificateUtils.generateX509Certificate(caData);
		} catch (CertificateException e) {
			logger.error("Certificate Authority not valid : {}", e.getMessage(), e);
			throw new ServerFault("Certificate Authority not valid : " + e.getMessage(), e);
		}
	}

	@Override
	public void create(SmimeCacert obj) {
		validate(obj);
	}

	@Override
	public void update(SmimeCacert oldValue, SmimeCacert newValue) {
		validate(newValue);
	}
}

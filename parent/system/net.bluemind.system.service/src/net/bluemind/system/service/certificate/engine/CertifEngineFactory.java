/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.service.certificate.engine;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public abstract class CertifEngineFactory {

	private static final Logger logger = LoggerFactory.getLogger(CertifEngineFactory.class);

	public static Optional<ICertifEngine> get(String domainUid) {

		try {
			CertificateDomainEngine sslCertifEngine = CertificateDomainEngine

					.valueOf(new SecurityCertificateHelper().getSslCertifEngine(domainUid));
			switch (sslCertifEngine) {
			case LETS_ENCRYPT:
				return Optional.of(new LetsEncryptCertifEngine(domainUid));
			case FILE:
				return Optional.of(new CertFileCertifEngine(domainUid));
			case DISABLED:
				return Optional.of(new DisabledCertifEngine(domainUid));
			default:
				throw new ServerFault(
						String.format("SSL Certificate Engine settings '%s' is not valid.", sslCertifEngine));
			}
		} catch (IllegalArgumentException e) {
			String sslCertifEngineValues = Stream.of(CertificateDomainEngine.values()).map(arr -> arr.name())
					.collect(Collectors.joining(", ", "[", "]"));
			logger.warn("SSL Certificate Engine authorized values are : {}", sslCertifEngineValues);
		}
		return Optional.empty();
	}

	public static ICertifEngine get(CertData certData, BmContext context) {

		try {
			switch (certData.sslCertificateEngine) {
			case LETS_ENCRYPT:
				return new LetsEncryptCertifEngine(certData, context);
			case FILE:
				return new CertFileCertifEngine(certData, context);
			case DISABLED:
				return new DisabledCertifEngine(certData, context);
			default:
				throw new ServerFault(String.format("SSL Certificate Engine settings '%s' is not valid.",
						certData.sslCertificateEngine));
			}
		} catch (IllegalArgumentException e) {
			String sslCertifEngineValues = Stream.of(CertificateDomainEngine.values()).map(arr -> arr.name())
					.collect(Collectors.joining(", ", "[", "]"));
			throw new ServerFault("SSL Certificate Engine authorized values are : " + sslCertifEngineValues);
		}
	}

}

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
package net.bluemind.system.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CertData {

	@BMApi(version = "3")
	public enum CertificateDomainEngine {
		FILE("Files"), LETS_ENCRYPT("Let's Encrypt"), DISABLED("Disabled");

		private final String description;

		CertificateDomainEngine(String description) {
			this.description = description;
		}

		public String description() {
			return description;
		}
	}

	public CertificateDomainEngine sslCertificateEngine;
	public String certificateAuthority;
	public String certificate;
	public String privateKey;
	public String domainUid;
	public String email;

	public static CertData create(CertificateDomainEngine sslCertificateEngine, String certificateAuthority,
			String certificate, String privateKey, String domainUid, String email) {

		CertData ret = new CertData();
		ret.sslCertificateEngine = sslCertificateEngine;
		ret.certificateAuthority = certificateAuthority;
		ret.certificate = certificate;
		ret.privateKey = privateKey;
		ret.domainUid = domainUid;
		ret.email = email;
		return ret;
	}

	/**
	 * Create CertData with domainUid and without email
	 *
	 * @param sslCertificateEngine
	 * @param certificateAuthority
	 * @param certificate
	 * @param privateKey
	 * @param domainUid
	 * @return created {@link CertData}
	 */
	public static CertData createWithDomainUid(CertificateDomainEngine sslCertificateEngine,
			String certificateAuthority, String certificate, String privateKey, String domainUid) {
		return CertData.create(sslCertificateEngine, certificateAuthority, certificate, privateKey, domainUid, null);
	}

	/**
	 * Create CertData with default domainUid = "global.virt" and without email
	 *
	 * @param sslCertificateEngine
	 * @param certificateAuthority
	 * @param certificate
	 * @param privateKey
	 * @return created {@link CertData}
	 */
	public static CertData defaultCreate(CertificateDomainEngine sslCertificateEngine, String certificateAuthority,
			String certificate, String privateKey) {
		return CertData.createWithDomainUid(sslCertificateEngine, certificateAuthority, certificate, privateKey,
				"global.virt");
	}

	/**
	 * Create CertData for LETS_ENCRYPT ssl certif engine with domainUid and email,
	 * but without any files
	 *
	 * @param domainUid
	 * @param email
	 * @return created {@link CertData}
	 */
	public static CertData createForLetsEncrypt(String domainUid, String email) {
		return CertData.create(CertificateDomainEngine.LETS_ENCRYPT, null, null, null, domainUid, email);
	}

	/**
	 * Create CertData for DISABLED ssl certif engine with domainUid only
	 *
	 * @param domainUid
	 * @return created {@link CertData}
	 */
	public static CertData createForDisable(String domainUid) {
		return CertData.create(CertificateDomainEngine.DISABLED, null, null, null, domainUid, null);
	}

}

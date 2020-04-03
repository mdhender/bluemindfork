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
package net.bluemind.directory.hollow.datamodel.utils;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pem {

	private static final Logger logger = LoggerFactory.getLogger(Pem.class);
	public final String pem;

	public Pem(String pem) {
		this.pem = pem;
	}

	public Optional<byte[]> toPcks7() {
		if (pem == null) {
			return Optional.empty();
		}
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			Certificate x509 = cf.generateCertificate(new ByteArrayInputStream(pem.getBytes()));

			X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(x509.getEncoded());
			Store<?> certStore = new JcaCertStore(Arrays.asList(x509CertificateHolder));

			CMSProcessableByteArray msg = new CMSProcessableByteArray("signed data".getBytes());
			CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
			gen.addCertificates(certStore);
			CMSSignedData data = gen.generate(msg);
			return Optional.of(data.getEncoded());
		} catch (Exception e) {
			logger.warn("Cannot transform PEM to PKCS7", e);
			return Optional.empty();
		}
	}

	public Optional<byte[]> toDer() {
		if (pem == null) {
			return Optional.empty();
		}
		String base64Content = pem.replaceAll("\\s", "");
		base64Content = base64Content.replace("-----BEGINCERTIFICATE-----", "");
		base64Content = base64Content.replace("-----ENDCERTIFICATE-----", "");
		return Optional.of(Base64.getDecoder().decode(base64Content.getBytes()));
	}

}

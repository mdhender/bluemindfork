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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.smime.cacerts.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.BmContext;
import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.system.service.helper.SecurityCertificateHelper;
import net.bluemind.utils.CertificateUtils;

public class CrlReader {
	private static final Logger logger = LoggerFactory.getLogger(CrlReader.class);

	private class CRLEntry {
		private final X509CRL crlX509;
		private final String url;

		public CRLEntry(X509CRL crlX509, String url) {
			this.crlX509 = crlX509;
			this.url = url;
		}

		public List<SmimeRevocation> createRevocations() {
			Set<? extends X509CRLEntry> revokedCertificates = crlX509.getRevokedCertificates();
			if (revokedCertificates == null || revokedCertificates.isEmpty()) {
				logger.info("S/MIME CA certificate {} has no revoked certificates", cacertUid);
				return Collections.emptyList();
			}

			return revokedCertificates.stream().map(crlEntry -> createRevocation(crlEntry)).toList();
		}

		private SmimeRevocation createRevocation(X509CRLEntry crlEntry) {
			return SmimeRevocation.create(crlEntry.getSerialNumber().toString(), crlEntry.getRevocationDate(),
					crlEntry.getRevocationReason() != null ? crlEntry.getRevocationReason().name() : null, url,
					crlX509.getThisUpdate(), crlX509.getNextUpdate(), issuer, cacertUid);
		}
	}

	private Set<CRLEntry> crls = new HashSet<>();
	private X509Certificate caCert;
	private String cacertUid;
	private String issuer;
	private SecurityCertificateHelper systemHelper;

	public CrlReader(BmContext context, X509Certificate caCert, String cacertUid) {
		this.systemHelper = new SecurityCertificateHelper(context);
		this.caCert = caCert;
		this.cacertUid = cacertUid;
		this.issuer = issuerWithOids();
	}

	private String issuerWithOids() {
		try {
			X500Name x500name = new JcaX509CertificateHolder(caCert).getIssuer();
			RDN[] rdNs = x500name.getRDNs();
			return Arrays.asList(rdNs).stream()
					.map(r -> r.getFirst().getType().toString().concat("=").concat(r.getFirst().getValue().toString()))
					.collect(Collectors.joining(","));
		} catch (CertificateEncodingException e) {
            logger.warn("Error occured trying to read CA issuer : {}", e.getMessage());
		}
		return caCert.getIssuerX500Principal().getName(X500Principal.RFC1779);
	}

	public void read(InputStream crlStream) {
		try {
			CRLEntry crlEntry = readCrlFile(crlStream, null);
			crls.add(crlEntry);
		} catch (Exception e) {
			logger.warn("Error occurs trying to read CRL stream : {}", e.getMessage());
		}
	}

	public void read(String url) {
		try {
			CRLEntry crlEntry = downloadCRLFromWeb(url);
			crls.add(crlEntry);
		} catch (Exception e) {
			logger.warn("Error occurs trying to read CRL from url:{} : {}", url, e.getMessage());
		}
	}

	public List<SmimeRevocation> getRevocations() {
		List<SmimeRevocation> revocations = new ArrayList<>();
		try {
			CertificateUtils.getCrlDistributionPoints(caCert).forEach(url -> {
				read(url);
			});
			revocations.addAll(createRevocations());
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
		return revocations;
	}

	private CRLEntry readCrlFile(InputStream crlStream, String url) throws Exception {
		X509CRL crl = (X509CRL) CertificateUtils.generateX509Crl(crlStream);
		crl.verify(caCert.getPublicKey());
		verifyIssuer(crl);
		return new CRLEntry(crl, url);
	}

	private void verifyIssuer(X509CRL crl) throws CRLException {
		if (!Arrays.equals(caCert.getIssuerX500Principal().getEncoded(), crl.getIssuerX500Principal().getEncoded())) {
			throw new CRLException("CRL Issuer is not valid.");
		}
	}

	private HttpURLConnection connect(String url) throws MalformedURLException, IOException {
		Proxy proxy = systemHelper.configureProxySession();
		if (proxy == null) {
			return (HttpURLConnection) new URL(url).openConnection();
		} else {
			return (HttpURLConnection) new URL(url).openConnection(proxy);
		}
	}

	private CRLEntry downloadCRLFromWeb(String url) throws Exception {
		try (InputStream crlStream = connect(url).getInputStream()) {
			return readCrlFile(crlStream, url);
		}
	}

	public List<SmimeRevocation> createRevocations() {
		List<SmimeRevocation> revocations = new ArrayList<>();
		crls.forEach(crlEntry -> {
			if (crlEntry.crlX509 != null) {
				revocations.addAll(crlEntry.createRevocations());
			}
		});
		return revocations;
	}

}
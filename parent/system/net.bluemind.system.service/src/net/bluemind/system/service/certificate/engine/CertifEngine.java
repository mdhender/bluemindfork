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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public abstract class CertifEngine implements ICertifEngine {

	protected static final Logger logger = LoggerFactory.getLogger(CertifEngine.class);

	protected String domainUid;
	protected CertData certData;
	protected final SecurityCertificateHelper systemHelper;
	protected ItemValue<Domain> domain;

	private CertifEngine() {
		this.systemHelper = new SecurityCertificateHelper();
	}

	private CertifEngine(BmContext context) {
		this.systemHelper = context != null ? new SecurityCertificateHelper(context) : new SecurityCertificateHelper();
	}

	public CertifEngine(String domainUid) {
		this();
		this.domainUid = domainUid;
		verifyDomain();
	}

	public CertifEngine(CertData certData, BmContext context) {
		this(context);
		this.domainUid = certData.domainUid;
		this.certData = certData;
		verifyCertDataAndDomain();
	}

	@Override
	public abstract void externalUrlUpdated(boolean removed);

	@Override
	public abstract boolean authorizeUpdate();

	@Override
	public abstract void certificateMgmt(List<ItemValue<Server>> servers, List<ISystemHook> hooks);

	protected void fireCertificateUpdated(List<ISystemHook> hooks) {
		for (ISystemHook hook : hooks) {
			hook.onCertificateUpdate();
		}
	}

	private void verifyCertDataAndDomain() {
		if (certData == null || Strings.isNullOrEmpty(domainUid) || certData.sslCertificateEngine == null) {
			throw new ServerFault("Missing data from request body");
		}
		verifyDomain();
	}

	private void verifyDomain() {
		domain = systemHelper.checkDomain(domainUid);
	}

	/**
	 * Create CertData with CertificateDomainEngine and domainUid
	 *
	 * @param sslCertifEngine
	 * @return created CertData
	 */
	protected CertData createDomainCertData(CertificateDomainEngine sslCertifEngine) {
		return CertData.create(sslCertifEngine, null, null, null, domainUid, null);
	}

	protected void checkCertificateAndWriteFile(List<ItemValue<Server>> servers, List<ISystemHook> hooks) {
		logger.info("update certificate by {}", systemHelper.getContext().getSecurityContext().getSubject());
		checkCertificate();
		for (ItemValue<Server> serverItem : servers) {
			writeCert(serverItem.value);
		}
		updateDomainCertifEngine();
		fireCertificateUpdated(hooks);
	}

	private void writeCert(Server server) {
		String ca = certData.certificateAuthority;
		String cert = certData.certificate;
		String pkey = certData.privateKey;
		String domainUid = certData.domainUid;

		logger.info("Writing certificate for domain {} ", domainUid);
		String certPlusKey = cert + "\n" + pkey + "\n" + ca;
		INodeClient nc = new AHCNodeClientFactory().create(server.address());
		copyCertToNode(nc, ca, certPlusKey);
	}

	private void copyCertToNode(INodeClient nc, String ca, String certPlusKey) {
		if (Strings.isNullOrEmpty(domainUid) || SecurityCertificateHelper.isGlobalVirtDomain(domainUid)) {
			TaskRef tr = nc.executeCommand("mkdir -p /var/lib/bm-ca");
			NCUtils.waitFor(nc, tr);
			nc.writeFile("/var/lib/bm-ca/cacert.pem", new ByteArrayInputStream(ca.getBytes()));
		}

		TaskRef tr = nc.executeCommand("mkdir -p /etc/bm/certs");
		NCUtils.waitFor(nc, tr);
		for (String certFilePath : getCertificateFilePaths()) {
			nc.writeFile(certFilePath, new ByteArrayInputStream(certPlusKey.getBytes()));
		}
	}

	protected void updateDomainCertifEngine() {
		String sslCertificateEngine = certData.sslCertificateEngine.name();
		logger.info("update ssl_certif_engine for domain " + domainUid);
		if (SecurityCertificateHelper.isGlobalVirtDomain(domainUid)) {
			Map<String, String> settings = systemHelper.getGlobalSettingsService().getValues().values;
			settings.put(SysConfKeys.ssl_certif_engine.name(), sslCertificateEngine);
			systemHelper.getGlobalSettingsService().updateMutableValues(settings);
		} else {
			Map<String, String> settings = systemHelper.getDomainSettingsService(domainUid).get();
			settings.put(DomainSettingsKeys.ssl_certif_engine.name(), sslCertificateEngine);
			systemHelper.getDomainSettingsService(domainUid).set(settings);
		}
	}

	public List<String> getCertificateFilePaths() {
		String bmCertFileName = "bm_cert.pem";
		if (!SecurityCertificateHelper.isGlobalVirtDomain(domainUid)) {
			bmCertFileName = "bm_cert-" + domainUid + ".pem";
		}
		return Arrays.asList("/etc/bm/certs/" + bmCertFileName, "/etc/ssl/certs/" + bmCertFileName);
	}

	@Override
	public void checkCertificate() {
		if (Strings.isNullOrEmpty(certData.certificate)) {
			throw new ServerFault("Certificate must not be null or empty");
		}

		if (Strings.isNullOrEmpty(certData.privateKey)) {
			throw new ServerFault("Private key must not be null or empty");
		}

		if (Strings.isNullOrEmpty(certData.certificateAuthority)) {
			throw new ServerFault("CA must not be null or empty");
		}

		byte[] caData = certData.certificateAuthority.getBytes();
		byte[] certificateData = certData.certificate.getBytes();
		byte[] pkeyData = certData.privateKey.getBytes();

		CertificateFactory cf = null;
		try {
			cf = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new ServerFault("CertificateFactory not available", e);
		}

		// loading CAs
		Collection<? extends Certificate> certificates = null;
		try {
			certificates = cf.generateCertificates(new ByteArrayInputStream(caData));
		} catch (CertificateException e) {
			logger.error("error during ca read : {}", e.getMessage(), e);
			throw new ServerFault("Certificate Authority not valid : " + e.getMessage(), e);
		}

		Set<TrustAnchor> tA = new HashSet<>();
		List<X509Certificate> certList = new ArrayList<>();
		for (Certificate aCertificate : certificates) {
			X509Certificate certificate = (X509Certificate) aCertificate;

			if (!certificate.getSubjectDN().equals(certificate.getIssuerDN())
					&& certificate.getBasicConstraints() == -1) {
				throw new ServerFault("Certificate Authority is not one");
			}
			logger.info("CA issuer {} for {} depth {} ", certificate.getIssuerX500Principal(),
					certificate.getSubjectX500Principal(), certificate.getBasicConstraints());

			// add CA to trusted anchors
			tA.add(new TrustAnchor(certificate, null));
			certList.add(certificate);
		}

		// load certificate
		X509Certificate cert = null;
		try {
			cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
		} catch (CertificateException e) {
			logger.error("error reading certificate: {}", e.getMessage(), e);
			throw new ServerFault("Certificate not valid : " + e.getMessage(), e);
		}

		// verify cert is valid against trused CAs (caData)
		try {
			CertPath cp = cf.generateCertPath(Arrays.asList(cert));

			// list of trusted CA (from caData)
			PKIXParameters pkixp = new PKIXParameters(tA);
			pkixp.setRevocationEnabled(false);

			CertPathValidator cpv = CertPathValidator.getInstance("PKIX");

			cpv.validate(cp, pkixp);
		} catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
			logger.error("error during cert validation {}", e.getMessage(), e);
			throw new ServerFault("Certificate not valid : " + e.getMessage(), e);
		} catch (CertPathValidatorException e) {
			logger.error("error during cert validation {}", e.getMessage(), e);
			throw new ServerFault("Certificate path not valid : " + e.getMessage(), e);
		}

		logger.info("Certificate issuer {} for {} ", cert.getIssuerX500Principal(), cert.getSubjectX500Principal());
		if (cert.getBasicConstraints() != -1) {
			// not a CA
			throw new ServerFault("Certificate is not a certificate but a CA");
		}

		// finally validate the key against certificate

		// load privatekey
		PrivateKey pk = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");

			@SuppressWarnings("resource")
			Object o = new PEMParser(new StringReader(new String(pkeyData))).readObject();
			PrivateKeyInfo privateKeyInfo = null;
			if (o instanceof PEMKeyPair) {
				PEMKeyPair keyPair = (PEMKeyPair) o;
				privateKeyInfo = keyPair.getPrivateKeyInfo();
			} else if (o instanceof PrivateKeyInfo) {
				privateKeyInfo = (PrivateKeyInfo) o;
			} else if (o == null) {
				throw new ServerFault("privatekey format not handled");
			} else {
				throw new ServerFault("privatekey format not handled " + o.getClass().getName());
			}

			pk = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded()));
		} catch (NoSuchAlgorithmException e) {
			logger.error("error during pk validation: {}", e.getMessage(), e);
			throw new ServerFault("error during private key validation", e);
		} catch (InvalidKeySpecException e) {
			logger.error("error loading private key: {}", e.getMessage(), e);
			throw new ServerFault("error loading private key : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("error during private key validation: {}", e.getMessage(), e);
			throw new ServerFault("error during private key validation ", e);
		}

		try {
			Signature dsa = Signature.getInstance("SHA1withRSA");
			dsa.initSign(pk);
			dsa.update("testSign".getBytes());
			byte[] signature = dsa.sign();

			dsa = Signature.getInstance("SHA1withRSA");
			dsa.initVerify(cert.getPublicKey());
			dsa.update("testSign".getBytes());
			if (!dsa.verify(signature)) {
				throw new ServerFault("private key doesnt correspond to certificate");
			}

		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			logger.error("Error occurred during private key validation: {}", e.getMessage(), e);
			throw new ServerFault("Error occurred during private key validation : " + e.getMessage(), e);
		}
	}

}

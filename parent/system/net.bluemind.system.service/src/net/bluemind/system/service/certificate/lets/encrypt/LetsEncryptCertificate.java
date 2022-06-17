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
package net.bluemind.system.service.certificate.lets.encrypt;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Metadata;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.toolbox.AcmeUtils;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.api.CertData;
import net.bluemind.system.service.certificate.engine.ICertifEngine;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public class LetsEncryptCertificate {

	private static final Logger logger = LoggerFactory.getLogger(LetsEncryptCertificate.class);

	private static final int RSA_KEY_SIZE = 2048;
	private static final String LETS_ENCRYPT_PROD_SERVER = "acme://letsencrypt.org";
	private static final String LETS_ENCRYPT_TEST_SERVER = "acme://letsencrypt.org/staging";
	public static final String CHALLENGE_LOCATION = "/var/www/letsencrypt/";
	private static final String LETS_ENCRYPT_STAGING_LOCATION = "/etc/bm/lets-encrypt.staging";

	public static final String CERT_END_DATE_FORMAT = "yyyy-MM-dd";

	private Certificate certificate;
	private String letsEncryptServer;

	private SecurityCertificateHelper systemHelper;
	private ICertifEngine certifEngine;

	private enum LetsEncryptProperties {
		CERTIFICATE_END_DATE, TOS_APPROVAL, LETS_ENCRYPT_CONTACT;
	}

	public ICertifEngine getCertifEngine() {
		return certifEngine;
	}

	public LetsEncryptCertificate(ICertifEngine certifEngine, BmContext context) {
		this(context);
		this.certifEngine = certifEngine;
	}

	public LetsEncryptCertificate(BmContext context) {
		this.systemHelper = new SecurityCertificateHelper(context);
		init();
	}

	public LetsEncryptCertificate(SecurityCertificateHelper systemHelper) {
		this.systemHelper = systemHelper;
		init();
	}

	private void init() {
		Path path = Paths.get(LETS_ENCRYPT_STAGING_LOCATION);
		if (Files.exists(path)) {
			letsEncryptServer = LETS_ENCRYPT_TEST_SERVER;
		} else {
			letsEncryptServer = LETS_ENCRYPT_PROD_SERVER;
		}
		logger.info("Let's Encrypt URL server used: " + letsEncryptServer);
	}

	/**
	 * Generate a certificate for the given domain.
	 * <p>
	 * Save certificate end date and TOS approval to BM database
	 *
	 * @param monitor server task monitor
	 */
	public void letsEncrypt(IServerTaskMonitor monitor) {
		monitor.begin(10, "Start Let's Encrypt generation");
		if (!LetsEncryptCertificate.isTosApproved(certifEngine.getDomain().value)) {
			throw new ServerFault("Let's Encrypt terms of service must been approved to continue");
		}

		String domainUid = certifEngine.getDomain().uid;
		String domainExternalUrl = systemHelper.getExternalUrl(domainUid);

		if (Strings.isNullOrEmpty(certifEngine.getCertData().email)) {
			certifEngine.getCertData().email = "no-reply@" + systemHelper.getDefaultDomain(domainUid);
		}
		monitor.progress(1, "Verifications done continue...");

		Proxy proxy = systemHelper.configureProxySession();
		fetchCertificate(certifEngine.getCertData(), domainExternalUrl, proxy, monitor);

		if (certificate != null && certificate.getCertificate() != null) {
			certifEngine.getDomain().value.properties.put(LetsEncryptProperties.CERTIFICATE_END_DATE.name(),
					new SimpleDateFormat(CERT_END_DATE_FORMAT).format(certificate.getCertificate().getNotAfter()));
			certifEngine.getDomain().value.properties.put(LetsEncryptProperties.LETS_ENCRYPT_CONTACT.name(),
					certifEngine.getCertData().email);
			systemHelper.getDomainService().update(domainUid, certifEngine.getDomain().value);
		}
		monitor.progress(1, "Let's Encrypt certificate generated !");
	}

	public String getTermsOfService() {
		try {
			Metadata meta = createSession(letsEncryptServer).getMetadata();
			return meta.getTermsOfService().toString();
		} catch (AcmeException e) {
			throw new LetsEncryptException("Error occurred trying to get Let's Encrypt Terms of service", e);
		}
	}

	public void approveTermsOfService(String domainUid) {
		if (!Strings.isNullOrEmpty(domainUid)) {
			IDomains domainService = systemHelper.getDomainService();
			ItemValue<Domain> domainItem = domainService.get(domainUid);
			domainItem.value.properties.put(LetsEncryptProperties.TOS_APPROVAL.name(), "true");
			domainService.update(domainItem.uid, domainItem.value);
		}
	}

	public void cleanLetsEncryptProperties(String domainUid) {
		IDomains domainService = systemHelper.getDomainService();
		ItemValue<Domain> domain = domainService.get(domainUid);
		domain.value.properties.remove(LetsEncryptProperties.CERTIFICATE_END_DATE.name());
		domain.value.properties.remove(LetsEncryptProperties.TOS_APPROVAL.name());
		domain.value.properties.remove(LetsEncryptProperties.LETS_ENCRYPT_CONTACT.name());
		domainService.update(domain.uid, domain.value);
	}

	/**
	 * Generates a certificate for the given domains. Also takes care for the
	 * registration process.
	 *
	 * @param certData    {@link CertData} contains domains certificate data
	 * @param externalUrl domains external URL to get a common certificate for
	 * @param proxy       proxy to use to contact lets's encrypt
	 */
	private void fetchCertificate(CertData certData, String externalUrl, Proxy proxy, IServerTaskMonitor monitor) {
		Session session = createSession(letsEncryptServer);
		if (proxy != null) {
			session.networkSettings().setProxy(proxy);
		}
		monitor.progress(1, "Session created");

		KeyPair userKeyPair = KeyPairUtils.createKeyPair(RSA_KEY_SIZE);

		Account acct = findOrRegisterAccount(session, userKeyPair, certData.email);
		monitor.progress(1, "Account " + certData.email + " registered");
		KeyPair domainKeyPair = KeyPairUtils.createKeyPair(RSA_KEY_SIZE);

		monitor.progress(1, "Certificate ordered");
		Collection<String> domains = new HashSet<>();
		domains.add(externalUrl);
		domains.addAll(systemHelper.getOtherUrls(certifEngine.getDomain().uid).map(ou -> Arrays.asList(ou.split(" ")))
				.orElseGet(Collections::emptyList));
		Order order = creatingOrder(acct, domains);

		for (Authorization auth : order.getAuthorizations()) {
			authorize(auth);
		}
		monitor.progress(1, "Valid authorizations");

		CSRBuilder csrb = createCsr(domainKeyPair, domains);
		certificate = orderAndGetCertificate(order, csrb);
		String msg = String.format("Success! The certificate for domains '%s' has been generated!",
				domains.stream().map(n -> String.valueOf(n)).collect(Collectors.joining("-", "{", "}")));
		monitor.progress(1, msg);

		updateCertData(domainKeyPair, certData);
		logger.info(msg);
		logger.info("Certificate URL: {}", certificate.getLocation());
	}

	/**
	 * Order the certificate and wait for the order to complete
	 *
	 * @param order
	 * @param csrb
	 * @return the retrieved {@link Certificate}
	 */
	private Certificate orderAndGetCertificate(Order order, CSRBuilder csrb) {
		try {
			order.execute(csrb.getEncoded());
			try {
				int attempts = 10;
				while (order.getStatus() != Status.VALID && attempts-- > 0) {
					if (order.getStatus() == Status.INVALID) {
						logger.error("Order has failed, reason: {}", order.getError());
						throw new AcmeException("Order failed... Giving up.");
					}

					Thread.sleep(3000L);
					order.update();
				}
			} catch (InterruptedException ex) {
				logger.error("interrupted", ex);
				Thread.currentThread().interrupt();
			}
		} catch (Exception e) {
			throw new LetsEncryptException("Error occurred trying to get certificate from the order", e);
		}

		Certificate certificate = order.getCertificate();
		if (certificate == null) {
			throw new LetsEncryptException("No certificate has been retrieved from the order");
		}

		return certificate;
	}

	/**
	 * Generate a CSR for all of the domains, and sign it with the domain key pair.
	 *
	 * @param domainKeyPair the domain key
	 * @param domains       the domains list
	 * @return created {@link CSRBuilder}
	 */
	private CSRBuilder createCsr(KeyPair domainKeyPair, Collection<String> domains) {
		try {
			CSRBuilder csrb = new CSRBuilder();
			csrb.addDomains(domains);
			csrb.sign(domainKeyPair);
			return csrb;
		} catch (IOException e) {
			throw new LetsEncryptException("CSR generation failed", e);
		}
	}

	/**
	 * Create a session for Let's Encrypt
	 *
	 * @param url the Let's Encrypt URL to create session for
	 * @return created {@link Session}
	 */
	private static Session createSession(String url) {
		return new Session(url);
	}

	/**
	 * Order the certificate
	 *
	 * @param acct    the account
	 * @param domains the domains list
	 * @return created {@link Order}
	 */
	private Order creatingOrder(Account acct, Collection<String> domains) {
		try {
			return acct.newOrder().domains(domains).create();
		} catch (AcmeException e) {
			throw new LetsEncryptException("Order creation failed for domains "
					+ domains.stream().collect(Collectors.joining("-", "{", "}")) + " because: " + e.getMessage(), e);
		}
	}

	/**
	 * Finds your {@link Account} at the ACME server. It will be found by your
	 * user's public key. If your key is not known to the server yet, a new account
	 * will be created.
	 *
	 * @param session    {@link Session} to bind with
	 * @param accountKey {@link KeyPair} the user key
	 * @param email      the contact email
	 * @return {@link Account}
	 */
	private Account findOrRegisterAccount(Session session, KeyPair accountKey, String email) {
		try {
			Account account = new AccountBuilder() //
					.addEmail(email) //
					.agreeToTermsOfService() //
					.useKeyPair(accountKey) //
					.create(session);
			logger.info("Registered a new user, URL: {}", account.getLocation());
			return account;

		} catch (AcmeException e) {
			throw new LetsEncryptException("Account creation failed", e);
		}
	}

	/**
	 * Authorize a domain. It will be associated with your account, so you will be
	 * able to retrieve a signed certificate for the domain later.
	 *
	 * @param auth {@link Authorization} to perform
	 */
	private void authorize(Authorization auth) {
		final String domain = auth.getIdentifier().getDomain();
		logger.info("Authorization for domain {}", domain);

		if (auth.getStatus() == Status.VALID) {
			return;
		}

		Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
		if (challenge == null) {
			throw new LetsEncryptException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
		}

		createTokenFile(challenge, domain);

		if (challenge.getStatus() == Status.VALID) {
			return;
		}

		pollForChallengeToComplete(challenge, 10, domain);

	}

	private void pollForChallengeToComplete(Challenge challenge, int attempts, String domain) {
		try {
			challenge.trigger();
			while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
				if (challenge.getStatus() == Status.INVALID) {
					throw new LetsEncryptException("Challenge failed... Giving up because: " + challenge.getError());
				}
				Thread.sleep(3000L);
				challenge.update();
			}
		} catch (InterruptedException ex) {
			logger.error("interrupted", ex);
			Thread.currentThread().interrupt();
		} catch (AcmeException e) {
			throw new LetsEncryptException(e.getMessage());
		}

		if (challenge.getStatus() != Status.VALID) {
			throw new LetsEncryptException("Failed to pass the challenge for domain " + domain + ", ... Giving up.");
		}

		logger.info("Challenge has been completed. Remember to remove the validation resource.");
	}

	/**
	 * Create a file with a certain content to be reachable at a given path under
	 * the domain to be tested.
	 *
	 * @param challenge {@link Http01Challenge} the challenge to verify
	 * @param domain    the domain to be tested
	 */
	private void createTokenFile(Http01Challenge challenge, String domain) {
		try (FileWriter fw = new FileWriter(CHALLENGE_LOCATION + challenge.getToken())) {
			fw.write(challenge.getAuthorization());
			StringBuilder message = new StringBuilder();
			message.append("Challenge file created in your web server : ");
			message.append("http://").append(domain).append(CHALLENGE_LOCATION).append(challenge.getToken())
					.append("\n");
			message.append("Content: ").append(challenge.getAuthorization());
			logger.info(message.toString());
		} catch (IOException e) {
			throw new LetsEncryptException(
					String.format("Error occurred trying to create Token file for domain '%s'", domain), e);
		}
	}

	private void updateCertData(KeyPair domainKeyPair, CertData certData) {
		List<X509Certificate> certificateChains = certificate.getCertificateChain();
		if (certificateChains.size() < 2) {
			throw new LetsEncryptException("Error occurred trying to get chains certificate");
		}

		certData.certificate = getPemEncodedCertificate(Arrays.asList(certificateChains.get(0)));
		List<X509Certificate> certs = new ArrayList<>(certificateChains);
		certs.remove(0);
		certData.certificateAuthority = getPemEncodedCertificate(certs);
		certData.privateKey = getPemEncodedKey(domainKeyPair.getPrivate().getEncoded());
	}

	private String getPemEncodedCertificate(List<X509Certificate> certs) {
		try (Writer out = new StringWriter()) {
			for (X509Certificate cert : certs) {
				AcmeUtils.writeToPem(cert.getEncoded(), AcmeUtils.PemLabel.CERTIFICATE, out);
			}
			return out.toString();
		} catch (CertificateEncodingException | IOException ex) {
			throw new LetsEncryptException("Certificate Encoding error", ex);
		}
	}

	private String getPemEncodedKey(byte[] privateKey) {
		try (Writer out = new StringWriter()) {
			AcmeUtils.writeToPem(privateKey, AcmeUtils.PemLabel.PRIVATE_KEY, out);
			return out.toString();
		} catch (IOException ex) {
			throw new LetsEncryptException("Private Key Encoding error", ex);
		}
	}

	public static Date getCertificateEndDateProperty(Domain d) throws ParseException {
		return new SimpleDateFormat(CERT_END_DATE_FORMAT)
				.parse(d.properties.get(LetsEncryptProperties.CERTIFICATE_END_DATE.name()));
	}

	public static boolean isTosApproved(Domain d) {
		return "true".equals(d.properties.get(LetsEncryptProperties.TOS_APPROVAL.name()));
	}

	public static String getContactProperty(Domain d) {
		return d.properties.get(LetsEncryptProperties.LETS_ENCRYPT_CONTACT.name());
	}

}

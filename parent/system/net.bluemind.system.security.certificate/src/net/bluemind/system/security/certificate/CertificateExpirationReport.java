/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.security.certificate;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.AbstractVerticle;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.security.certificate.CertificateTaskHelper.Mail;
import net.bluemind.system.service.certificate.IInCoreSecurityMgmt;
import net.bluemind.utils.Trust;

public class CertificateExpirationReport extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(CertificateExpirationReport.class);

	public CertificateExpirationReport() {
		super();
	}

	@Override
	public void start() {
		LocalTime eightAM = LocalTime.MIDNIGHT.plusHours(8);
		LocalDate tomorrow = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
		LocalDateTime tomorrowEightAM = LocalDateTime.of(tomorrow, eightAM);
		long delay = Duration.between(Instant.now(), tomorrowEightAM.toInstant(ZoneOffset.UTC)).toMillis();
		VertxPlatform.getVertx().setTimer(delay, this::execute);
	}

	private void execute(long id) {
		CompletableFuture.runAsync(this::checkExpiration);
		VertxPlatform.executeBlockingPeriodic(TimeUnit.DAYS.toMillis(1), timerId -> this.checkExpiration());
	}

	private void checkExpiration() {
		try {
			Set<String> urls = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IInCoreSecurityMgmt.class).getDomainExternalUrls().keySet();

			Optional.ofNullable(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(ISystemConfiguration.class).getValues().values.get(SysConfKeys.external_url.name()))
					.ifPresent(urls::add);

			for (String urlStr : urls) {
				URL url = new URL("https://" + urlStr);
				logger.info("Connecting to {}", url);

				HttpsURLConnection con = null;
				try {
					SSLContext context = SSLContext.getInstance("TLS");
					context.init(null, new X509TrustManager[] { Trust.createTrustManager() }, new SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
					con = (HttpsURLConnection) url.openConnection();
					con.setHostnameVerifier(Trust.acceptAllVerifier());
					con.connect();
					Certificate[] certs = con.getServerCertificates();
					for (Certificate cert : certs) {
						X509Certificate x509Certificate = (X509Certificate) cert;
						Date expirationDate = x509Certificate.getNotAfter();
						String dn = x509Certificate.getSubjectX500Principal().getName();
						String issuerX500Principal = x509Certificate.getIssuerX500Principal().getName();
						int validityInDays = CertificateTaskHelper.getDifferenceDays(new Date(), expirationDate);
						switch (validityInDays) {
						case 60:
							if (!issuerX500Principal.toLowerCase().contains("o=let's encrypt")) {
								sendAlert(validityInDays, dn);
							}
							break;
						case 29:
						case 7:
						case 1:
							sendAlert(validityInDays, dn);
							break;
						default:
							logger.info("Certificate {} is valid for {} days", dn, validityInDays);
							break;
						}
					}
				} finally {
					if (con != null) {
						con.disconnect();
					}
					HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
				}
			}
		} catch (Exception e) {
			logger.warn("Cannot check certificate expiration date", e);
		}
	}

	private void sendAlert(int validityInDays, String dn) {
		logger.warn("Certificate {} is valid for {} days", dn, validityInDays);

		Template template;
		try {
			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(this.getClass(), "/template");
			template = cfg.getTemplate("CertificateExpired.ftl");
		} catch (IOException e) {
			throw new ServerFault(e);
		}

		Mail mail = CertificateTaskHelper.generateMail(validityInDays, dn, template, null);

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		List<String> domains = provider.instance(IDomains.class).all().stream().map(itemValue -> itemValue.value.name)
				.filter(domainName -> !domainName.equals("global.virt")).collect(Collectors.toList());
		CertificateTaskHelper.sendEmailToSubscriptionContacts(
				provider.instance(ISystemConfiguration.class).getValues().stringValue(SysConfKeys.external_url.name()),
				domains, mail);
	}

}

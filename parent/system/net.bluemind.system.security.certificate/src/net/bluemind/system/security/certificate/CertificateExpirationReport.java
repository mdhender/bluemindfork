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
import java.io.StringWriter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.vertx.core.AbstractVerticle;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
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
			Optional<String> externalUrl = Optional.ofNullable(
					ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
							.getValues().values.get(SysConfKeys.external_url.name()));

			if (externalUrl.isPresent()) {
				URL url = new URL("https://" + externalUrl.get());
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
						int validityInDays = getDifferenceDays(new Date(), expirationDate);
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
					con.disconnect();
					HttpsURLConnection.setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
				}
			}
		} catch (Exception e) {
			logger.warn("Cannot check certificate expiration date", e);
		}
	}

	private void sendAlert(int validityInDays, String dn) {
		logger.warn("Certificate {} is valid for {} days", dn, validityInDays);

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		List<String> recipients = provider.instance(IInstallation.class).getSubscriptionContacts();

		String lang = provider.instance(IGlobalSettings.class).get().get("lang");
		if (lang == null || lang.isEmpty()) {
			lang = "en";
		}

		List<String> domains = provider.instance(IDomains.class).all().stream().map(itemValue -> itemValue.value.name)
				.filter(domainName -> !domainName.equals("global.virt")).collect(Collectors.toList());

		Mail mail = generateMail(lang, validityInDays, dn);

		for (String recipient : recipients) {
			String[] splitted = recipient.split("@");
			String recipientName = splitted[0];
			String recipientDomain = splitted[1];

			String senderDomain;

			if (domains.contains(recipientDomain)) {
				senderDomain = recipientDomain;
			} else {
				senderDomain = provider.instance(ISystemConfiguration.class).getValues()
						.stringValue(SysConfKeys.external_url.name());
			}

			sendMessage(mail, "no-reply", senderDomain, recipientName, recipientDomain);
		}
	}

	private Mail generateMail(String lang, int validityInDays, String dn) {
		Template template;
		try {
			Configuration cfg = new Configuration();
			cfg.setClassForTemplateLoading(this.getClass(), "/template");
			template = cfg.getTemplate("CertificateExpired.ftl");
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		MessagesResolver messageResolver = new MessagesResolver(
				ResourceBundle.getBundle("certificate_expiration", new Locale(lang)));
		Mail mail = new Mail();
		mail.subject = messageResolver.translate("expiration.subject", new Object[] {});

		StringWriter sw = new StringWriter();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("validity", validityInDays);
		data.put("dn", dn);
		data.put("msg", new FreeMarkerMsg(messageResolver));
		try {
			template.process(data, sw);
		} catch (IOException | TemplateException e) {
			throw new ServerFault(e);
		}
		sw.flush();

		mail.body = sw.toString();
		return mail;
	}

	private void sendMessage(Mail mail, String userFrom, String domainFrom, String userNameTo, String domainTo)
			throws ServerFault {
		logger.info("send message {} to admin {}@{}", mail.subject, userNameTo, domainTo);
		net.bluemind.core.sendmail.Mail mm = new net.bluemind.core.sendmail.Mail();
		mm.from = new Mailbox(userFrom, domainFrom);
		mm.html = mail.body;
		mm.subject = mail.subject;
		mm.to = SendmailHelper.formatAddress(userNameTo, userNameTo + "@" + domainTo);
		SendmailResponse send = new Sendmail().send(mm);
		if (send.isError()) {
			logger.warn("Cannot send certificate validity info. code: {}", send.code());
		}
	}

	public static int getDifferenceDays(Date d1, Date d2) {
		long diff = d2.getTime() - d1.getTime();
		return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}

	public static final class Mail {
		public String subject;
		public String body;
	}

}

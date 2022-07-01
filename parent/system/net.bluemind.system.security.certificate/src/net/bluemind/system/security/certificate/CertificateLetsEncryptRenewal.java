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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.AbstractVerticle;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.security.certificate.CertificateTaskHelper.Mail;
import net.bluemind.system.service.certificate.IInCoreSecurityMgmt;
import net.bluemind.system.service.certificate.engine.CertifEngineFactory;
import net.bluemind.system.service.certificate.engine.ICertifEngine;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;

public class CertificateLetsEncryptRenewal extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(CertificateLetsEncryptRenewal.class);

	public CertificateLetsEncryptRenewal() {
		super();
	}

	@Override
	public void start() {
		LocalTime sevenAM = LocalTime.MIDNIGHT.plusHours(7);
		LocalDate tomorrow = LocalDate.now(ZoneId.of("UTC")).plusDays(1);
		LocalDateTime tomorrowSevenAM = LocalDateTime.of(tomorrow, sevenAM);
		long delay = Duration.between(Instant.now(), tomorrowSevenAM.toInstant(ZoneOffset.UTC)).toMillis();
		VertxPlatform.getVertx().setTimer(delay, this::execute);
	}

	private void execute(long id) {
		CompletableFuture.runAsync(this::checkExpiration);
		VertxPlatform.executeBlockingPeriodic(TimeUnit.DAYS.toMillis(1), timerId -> this.checkExpiration());
	}

	private void checkExpiration() {
		Map<String, ItemValue<Domain>> mapDomainByUrl = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInCoreSecurityMgmt.class).getLetsEncryptDomainExternalUrls();

		for (Entry<String, ItemValue<Domain>> urlWithDomain : mapDomainByUrl.entrySet()) {
			try {
				ItemValue<Domain> domainItem = urlWithDomain.getValue();
				Date certificateEndDate = LetsEncryptCertificate.getCertificateEndDateProperty(domainItem.value);

				String externalUrl = urlWithDomain.getKey();
				int validityInDays = CertificateTaskHelper.getDifferenceDays(new Date(), certificateEndDate);
				if (validityInDays > 5 && validityInDays < 30) {
					if (!renewCertificate(domainItem, externalUrl)) {
						logger.error("Let's Encrypt auto renewal certificate failed for domain {} ({})", domainItem.uid,
								domainItem.value.defaultAlias);
					}
				} else if (validityInDays <= 5) {
					if (!renewCertificate(domainItem, externalUrl)) {
						logger.error(
								"Let's Encrypt auto renewal certificate failed for domain {} ({}) - sending mail alert to {}",
								domainItem.uid, domainItem.value.defaultAlias,
								LetsEncryptCertificate.getContactProperty(domainItem.value));
						sendAlert(validityInDays, externalUrl,
								LetsEncryptCertificate.getContactProperty(domainItem.value), domainItem.value.name,
								"Renewal failed, please contact your support !");
					}
				} else {
					logger.info("Certificate {} is valid for {} days", externalUrl, validityInDays);
				}
			} catch (Exception e) {
				logger.warn("Cannot check certificate expiration date", e);
			}
		}
	}

	private boolean renewCertificate(ItemValue<Domain> d, String externalUrl) {
		return CertifEngineFactory.get(d.uid).filter(c -> {
			try {
				c.authorizeLetsEncrypt();
				return true;
			} catch (ServerFault sf) {
				logger.warn("Let's Encrypt is not enabled for domain {} ({})", d.uid, d.value.defaultAlias);
				return false;
			}
		}).map(ICertifEngine::getCertData).map(certData -> {
			certData.email = Optional.ofNullable(LetsEncryptCertificate.getContactProperty(d.value))
					.filter(e -> !e.isEmpty())
					.orElseThrow(() -> new ServerFault("Let's Encrypt contact email must be set",
							ErrorCode.INVALID_PARAMETER));

			ServerSideServiceProvider service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			if (TaskUtils.wait(service, service.instance(ISecurityMgmt.class).generateLetsEncrypt(certData),
					log -> logger.info(log)).state.equals(TaskStatus.State.InError)) {
				return false;
			}

			return true;
		}).orElseGet(() -> {
			logger.error("No CertifEngineFactory for domain {} ({})", d.uid, d.value.defaultAlias);
			return false;
		});
	}

	private void sendAlert(int validityInDays, String externalUrl, String contactEmail, String domainName,
			String errorMsg) {
		Template template;
		try {
			Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
			cfg.setClassForTemplateLoading(this.getClass(), "/template");
			template = cfg.getTemplate("CertificateRenewalError.ftl");
		} catch (IOException e) {
			throw new ServerFault(e);
		}

		Mail mail = CertificateTaskHelper.generateMail(validityInDays, externalUrl, template, errorMsg);

		sendEmailToLetsEncryptContact(mail, externalUrl, contactEmail);
		CertificateTaskHelper.sendEmailToSubscriptionContacts(externalUrl, Arrays.asList(domainName), mail);
	}

	private void sendEmailToLetsEncryptContact(Mail mail, String externalUrl, String email) {
		CertificateTaskHelper.sendMessage(mail, "no-reply", externalUrl, email);
	}

}

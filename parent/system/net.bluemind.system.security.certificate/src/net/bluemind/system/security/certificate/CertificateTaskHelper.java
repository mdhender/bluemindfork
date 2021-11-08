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
package net.bluemind.system.security.certificate;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.system.api.IInstallation;

public class CertificateTaskHelper {

	private static final Logger logger = LoggerFactory.getLogger(CertificateTaskHelper.class);

	static Mail generateMail(int validityInDays, String dn, Template template, String errorMsg) {

		MessagesResolver messageResolver = new MessagesResolver(
				ResourceBundle.getBundle("certificate_expiration", new Locale(getLang())));
		Mail mail = new Mail();
		mail.subject = messageResolver.translate("expiration.subject", new Object[] {});

		StringWriter sw = new StringWriter();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("validity", validityInDays);
		data.put("dn", dn);
		if (!Strings.isNullOrEmpty(errorMsg)) {
			data.put("error", errorMsg);
		}
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

	static void sendMessage(Mail mail, String userFrom, String domainFrom, String mailTo) throws ServerFault {
		logger.info("send message {} to admin {}", mail.subject, mailTo);
		net.bluemind.core.sendmail.Mail mm = new net.bluemind.core.sendmail.Mail();
		mm.from = new Mailbox(userFrom, domainFrom);
		mm.html = mail.body;
		mm.subject = mail.subject;
		String[] userNameToTab = mailTo.split("@");
		String userNameTo = "";
		if (userNameToTab.length == 2) {
			userNameTo = userNameToTab[0];
		}
		mm.to = SendmailHelper.formatAddress(userNameTo, mailTo);
		SendmailResponse send = new Sendmail().send(mm);
		if (send.isError()) {
			logger.warn("Cannot send certificate validity info. code: {}", send.code());
		}
	}

	static int getDifferenceDays(Date d1, Date d2) {
		long diff = d2.getTime() - d1.getTime();
		return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
	}

	static final class Mail {
		public String subject;
		public String body;
	}

	private static String getLang() {
		String lang = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGlobalSettings.class)
				.get().get("lang");
		if (lang == null || lang.isEmpty()) {
			lang = "en";
		}
		return lang;
	}

	static void sendEmailToSubscriptionContacts(String externalUrl, List<String> domainNames, Mail mail) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		List<String> recipients = provider.instance(IInstallation.class).getSubscriptionContacts();
		for (String recipient : recipients) {
			String[] splitted = recipient.split("@");
			String recipientDomain = splitted[1];

			String senderDomain;
			if (domainNames.contains(recipientDomain)) {
				senderDomain = recipientDomain;
			} else {
				senderDomain = externalUrl;
			}

			CertificateTaskHelper.sendMessage(mail, "no-reply", senderDomain, recipient);
		}
	}
}

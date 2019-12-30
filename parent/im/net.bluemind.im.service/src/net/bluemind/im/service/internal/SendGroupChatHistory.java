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
package net.bluemind.im.service.internal;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.im.api.IMMessage;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class SendGroupChatHistory implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SendGroupChatHistory.class);

	private ISendmail mailer;
	private List<IMMessage> history;
	private List<String> recipients;
	private String sender;

	public SendGroupChatHistory(String sender, List<IMMessage> history, List<String> recipients) {
		mailer = new Sendmail();
		this.history = history;
		this.sender = sender;
		this.recipients = recipients;
	}

	@Override
	public void run() {
		try {
			Message m = getMessage();
			mailer.send(SendmailCredentials.asAdmin0(), sender, m);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @return
	 * @throws ServerFault
	 */
	private Message getMessage() throws ServerFault {

		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		ItemValue<Domain> domain = domainService.findByNameOrAliases(sender.split("@")[1]);

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domain.uid);

		ItemValue<User> user = userService.byEmail(sender);

		Mailbox sender = SendmailHelper.formatAddress(user.value.contactInfos.identification.formatedName.value,
				user.value.defaultEmail().address);

		IUserSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, domain.uid);
		Map<String, String> settings = settingsService.get(user.uid);

		TimeZone tz = TimeZone.getTimeZone(settings.get("timezone"));

		HashSet<Mailbox> recipientsMbox = new HashSet<Mailbox>();
		for (String recip : recipients) {
			Mailbox mb = SendmailHelper.formatAddress(recip, recip);
			recipientsMbox.add(mb);
		}

		MessageImpl m = new MessageImpl();

		m.setDate(new Date());
		m.setSender(sender);
		m.setFrom(sender);
		m.setTo(recipientsMbox);

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		StringBuilder content = new StringBuilder();

		content.append("<html>");
		content.append("<head>");
		content.append("<style type=\"text/css\">");
		content.append("body {color: #666;font-size: small;font-family: arial, sans-serif}");
		content.append(".from {color:#CCC;text-align: right;padding-right:3px;vertical-align:top}");
		content.append("</style>");
		content.append("</head>");
		content.append("<body>");

		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		content.append("<table>");
		String prevFrom = "";
		long prevTs = 0;

		Calendar c = Calendar.getInstance(tz);
		Map<String, User> users = new HashMap<String, User>();
		for (IMMessage entry : history) {
			content.append("<tr>");
			if (!prevFrom.equals(entry.from) || entry.timestamp.getTime() - prevTs > 60000) {

				String from = entry.from;

				User u = null;
				if (users.containsKey(from)) {
					u = users.get(from);
				} else {
					ItemValue<User> itemUser = userService.byEmail(from);
					if (itemUser != null) {
						u = itemUser.value;
						users.put(from, u);
					} else {
						users.put(from, null);
					}
				}

				if (u != null) {
					from = u.contactInfos.identification.formatedName.value;
				}
				content.append("<td class=\"from\">");
				c.setTimeInMillis(entry.timestamp.getTime());
				c.add(Calendar.MILLISECOND, c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET));
				content.append(sdf.format(c.getTime()));
				content.append("</td>");
				content.append("<td class=\"from\">");
				content.append(from);
				content.append("</td>");

				prevFrom = entry.from;
			} else {
				content.append("<td></td><td></td>");
			}
			content.append("<td>");
			String msg = format(entry.body);
			content.append(msg);
			content.append("</td>");
			content.append("</tr>");
			prevTs = entry.timestamp.getTime();
		}
		content.append("</table>");

		content.append("</body>");
		content.append("</html>");

		StringBuilder sub = new StringBuilder(1024);
		sub.append("Chat with ");
		String sep = "";
		for (String k : users.keySet()) {
			sub.append(sep);
			sub.append(users.get(k).contactInfos.identification.formatedName.value);
			sep = ", ";
		}
		m.setSubject(sub.toString());

		TextBody body = bodyFactory.textBody(content.toString(), StandardCharsets.UTF_8);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("charset", "UTF-8");
		m.setBody(body, "text/html", params);
		m.setContentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);

		return m;
	}

	/**
	 * @param plain
	 * @return
	 */
	private String format(String plain) {
		StringBuilder sb = new StringBuilder();
		String escaped = plain;
		escaped = escaped.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		escaped = escaped.replaceAll("(?m)(?:^|\\G) ", "&nbsp;");
		escaped = escaped.replace("\r\n", "\n");
		escaped = escaped.replace("\n", "<br/>");
		sb.append(escaped);
		String ret = sb.toString();
		return ret;

	}
}

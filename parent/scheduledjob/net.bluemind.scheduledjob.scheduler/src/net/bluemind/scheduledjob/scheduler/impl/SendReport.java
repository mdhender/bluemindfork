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
package net.bluemind.scheduledjob.scheduler.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.LogEntry;
import net.bluemind.scheduledjob.api.LogLevel;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class SendReport implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(SendReport.class);
	private ISendmail mailer;
	private RunIdImpl rid;
	private IJob service;

	public SendReport(RunIdImpl rid) {
		mailer = new Sendmail();
		this.rid = rid;

		try {
			service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void run() {
		try {
			Job job = service.getJobFromId(rid.jid);
			if (job != null && job.sendReport && !job.recipients.isEmpty()) {
				String domain = rid.domainName;

				String from = "no-reply@" + getNoReplyDomainName(domain);
				logger.info("Sending report using sender address {}", from);
				Message m = getMessage(rid, job, from);
				mailer.send(from, domain, m);
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private String getNoReplyDomainName(String domain) {
		if (StringUtils.isNotBlank(domain) && !domain.equals("global.virt")) {
			return domain;
		} else {
			ISystemConfiguration sysConf = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(ISystemConfiguration.class);
			SystemConf conf = sysConf.getValues();
			String externalUrl = conf.stringValue("external-url");
			if (StringUtils.isNotBlank(externalUrl)) {
				return externalUrl;
			} else {
				return domain;
			}
		}

	}

	private Message getMessage(RunIdImpl rid, Job job, String from) {
		Mailbox sender = SendmailHelper.formatAddress("Blue Mind Job Report", from);

		HashSet<Mailbox> to = new HashSet<Mailbox>();
		String[] recipients = job.recipients.split(" ");
		for (String s : recipients) {
			Mailbox mb = SendmailHelper.formatAddress(s, s);
			to.add(mb);
		}

		MessageImpl m = new MessageImpl();

		m.setDate(new Date());

		StringBuilder subject = new StringBuilder();
		subject.append("[Blue Mind Job Report]");
		subject.append(" - ");
		subject.append(rid.status.name());
		subject.append(" - ");
		subject.append(rid.domainName);
		subject.append(" - ");
		subject.append(rid.jid);

		m.setSubject(subject.toString());
		m.setSender(sender);
		m.setFrom(sender);
		m.setTo(to);

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		StringBuilder content = new StringBuilder();
		Set<LogEntry> entries = rid.entries;

		content.append("<html>");
		content.append("<head>");
		content.append("</head>");
		content.append("<body>");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		content.append("<p> Starts at: ");
		content.append(sdf.format(new Date(rid.startTime)));
		content.append("</p>");

		content.append("<p> Ends at: ");
		content.append(sdf.format(new Date(rid.endTime)));
		content.append("</p>");

		long duration = (rid.endTime - rid.startTime) / 1000;
		content.append("<p> Duration: ");
		content.append(duration);
		content.append(" sec.</p>");

		for (LogEntry entry : entries) {
			if ("en".equals(entry.locale)) {
				content.append("<p style='");
				content.append(getStringColor(entry.severity));
				content.append("'>");
				content.append("[");
				content.append(entry.severity);
				content.append("] ");
				content.append(sdf.format(new Date(entry.timestamp)));
				content.append(" ");
				content.append(entry.content);
				content.append("</p>");
			}
		}

		content.append("</body>");
		content.append("</html>");

		TextBody body = bodyFactory.textBody(content.toString());
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("charset", "UTF-8");
		params.put("format", "flowed");
		m.setBody(body, "text/html", params);
		m.setContentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);

		return m;
	}

	private String getStringColor(LogLevel level) {
		switch (level) {
		case ERROR:
			return "color: red;";
		case WARNING:
			return "color: orange;";
		default:
			return "";
		}
	}
}
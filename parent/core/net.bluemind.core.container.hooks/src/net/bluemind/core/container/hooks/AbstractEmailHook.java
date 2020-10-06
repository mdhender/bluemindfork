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
package net.bluemind.core.container.hooks;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.vertx.core.eventbus.EventBus;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendMailAddress;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUserSettings;

public abstract class AbstractEmailHook implements IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(AbstractEmailHook.class);

	protected Configuration cfg;
	private EventBus eventBus;

	public AbstractEmailHook() {
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(getClass(), "/templates");
		eventBus = VertxPlatform.eventBus();
	}

	public abstract void onAclChanged(BmContext context, ContainerDescriptor container,
			List<AccessControlEntry> previous, List<AccessControlEntry> current);

	protected void notify(BmContext context, ContainerDescriptor container, List<AccessControlEntry> entries,
			RawField... headers) throws ServerFault {

		if (entries.size() == 0) {
			logger.error("no one to notify");
			return;
		}

		SecurityContext sc = context.getSecurityContext();

		IDirectory fromDirService = context.provider().instance(IDirectory.class, container.domainUid);
		DirEntry fromDE = fromDirService.findByEntryUid(sc.getSubject());

		IDirectory memberDirService = context.provider().instance(IDirectory.class, container.domainUid);

		String fromDN = "";
		if (fromDE != null) {
			fromDN = fromDE.displayName;
		}

		HashMap<String, String> data = new HashMap<String, String>();
		data.put("user", fromDN);

		IUserSettings settingService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, container.domainUid);

		for (AccessControlEntry entry : entries) {
			if (entry.subject.equals(sc.getContainerUid())) {
				logger.debug("do not notify for public sharing");
				continue;
			}

			if (entry.subject.equals(container.owner)) {
				logger.debug("do not notify owner {}", entry.subject);
				continue;
			}

			final DirEntry targetedUser = memberDirService.findByEntryUid(entry.subject);

			if (targetedUser == null) {
				logger.error("Cannot find dirEntry {}", entry.subject);
				continue;
			}

			if (targetedUser.email == null) {
				if (targetedUser.kind == Kind.GROUP) {
					IGroup g = context.provider().instance(IGroup.class, container.domainUid);
					List<Member> members = g.getExpandedUserMembers(targetedUser.entryUid);
					members.forEach(m -> {
						DirEntry memberDE = memberDirService.findByEntryUid(m.uid);
						if (memberDE.email != null) {
							Map<String, String> prefs = settingService.get(memberDE.entryUid);
							String lang = prefs.get("lang");
							data.put("entity", I18nLabels.getInstance().translate(lang, container.name));
							Mailbox from = buildFrom(memberDE, sc);
							sendMessage(from, memberDE, this.getTemplateSubject(), this.getTemplateBody(), data, lang,
									headers);
						}
					});
				} else {
					logger.info("DirEntry {} has no email", targetedUser.displayName);
				}
				continue;
			}

			Mailbox from = buildFrom(targetedUser, sc);
			Map<String, String> prefs = settingService.get(targetedUser.entryUid);
			String lang = prefs.get("lang");

			data.put("entity", I18nLabels.getInstance().translate(lang, container.name));
			sendMessage(from, targetedUser, this.getTemplateSubject(), this.getTemplateBody(), data, lang, headers);
		}
	}

	private Mailbox buildFrom(DirEntry de, SecurityContext sc) {
		String noreply;
		if (de != null && de.email.contains("@")) {
			noreply = "no-reply@" + de.email.split("@")[1];
		} else {
			noreply = "no-reply@" + sc.getContainerUid();
		}
		return SendmailHelper.formatAddress(noreply, noreply);

	}

	private String buildSubject(String templateName, String locale, String user) {
		Map<String, Object> data = new HashMap<String, Object>();
		StringWriter sw = new StringWriter();

		data.put("user", user);
		Template t;
		try {
			t = getTemplate(templateName, locale);
			t.process(data, sw);
		} catch (TemplateException e1) {
			logger.error(e1.getMessage(), e1);
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}

		return sw.toString();
	}

	private Template getTemplate(String name, String locale) throws IOException {
		if (locale == null || !locale.equals("fr") && !locale.equals("en")) {
			locale = "en";
		}
		return cfg.getTemplate(name, new Locale(locale));
	}

	private void sendMessage(Mailbox from, DirEntry de, String templateSubject, String templateName,
			HashMap<String, String> data, String lang, RawField... headers) throws ServerFault {
		try {
			Mail m = new Mail();
			m.from = from;
			m.sender = from;
			m.to = SendmailHelper.formatAddress(de.displayName, de.email);
			m.subject = buildSubject(templateSubject, lang, data.get("user"));

			StringWriter sw = new StringWriter();
			Template t = getTemplate(templateName, lang);
			t.process(data, sw);
			sw.flush();

			m.html = sw.toString();
			for (RawField rh : headers) {
				m.headers.add(rh);
			}

			eventBus.publish(SendMailAddress.SEND, new LocalJsonObject<>(m));

		} catch (TemplateException | IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	abstract protected String getTemplateSubject();

	abstract protected String getTemplateBody();

}

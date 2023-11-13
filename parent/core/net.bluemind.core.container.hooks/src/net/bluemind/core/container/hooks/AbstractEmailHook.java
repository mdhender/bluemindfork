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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import net.bluemind.core.container.model.acl.Verb;
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
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUserSettings;

public abstract class AbstractEmailHook implements IAclHook {

	private static final Logger logger = LoggerFactory.getLogger(AbstractEmailHook.class);

	private static final String VIDEOCONFERENCE_DOCUMENTATION_URL = "https://forge.bluemind.net/confluence/display/BM4/Lier+une+videoconference+a+un+evenement";

	private static final EnumSet<Verb> sharingVerbs = EnumSet.of(Verb.All, Verb.Manage, Verb.Write, Verb.Read);

	private static final EnumSet<Verb> delegationVerbs = EnumSet.of(Verb.All, Verb.SendAs, Verb.SendOnBehalf);

	protected Configuration cfg;
	private EventBus eventBus;

	public AbstractEmailHook() {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(getClass(), "/templates");
		eventBus = VertxPlatform.eventBus();
	}

	protected void notify(BmContext context, ContainerDescriptor container, List<AccessControlEntry> entries,
			RawField... headers) throws ServerFault {

		if (entries.isEmpty()) {
			return;
		}

		SecurityContext sc = context.getSecurityContext();

		IDirectory dirService = context.provider().instance(IDirectory.class, container.domainUid);
		DirEntry fromDE = dirService.findByEntryUid(sc.getSubject());

		String fromDN = "";
		if (fromDE != null) {
			fromDN = fromDE.displayName;
		}

		HashMap<String, Object> data = new HashMap<>();
		data.put("user", fromDN);
		data.put("videoconfdocumentation", VIDEOCONFERENCE_DOCUMENTATION_URL);
		Set<String> types = new HashSet<String>(2);
		entries.stream().forEach(ace -> {
			if (sharingVerbs.contains(ace.getVerb())) {
				types.add("sharing");
			}
			if (delegationVerbs.contains(ace.getVerb())) {
				types.add("delegation");
			}
		});
		data.put("types", types);

		entries.stream().collect(Collectors.groupingBy(AccessControlEntry::getSubject)).entrySet().stream()
				.forEach(e -> sendMessageForSubject(e.getKey(), sc, container, dirService, context, data, headers));
	}

	private void sendMessageForSubject(String entrySubject, SecurityContext sc, ContainerDescriptor container,
			IDirectory dirService, BmContext context, HashMap<String, Object> data, RawField[] headers) {

		if (entrySubject.equals(sc.getContainerUid())) {
			logger.debug("do not notify for public sharing");
			return;
		}

		if (entrySubject.equals(container.owner)) {
			logger.debug("do not notify owner {}", entrySubject);
			return;
		}

		final DirEntry targetedUser = dirService.findByEntryUid(entrySubject);

		if (targetedUser == null) {
			logger.error("Cannot find dirEntry {}", entrySubject);
			return;
		}

		IUserSettings settingService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, container.domainUid);

		if (targetedUser.email == null) {
			if (targetedUser.kind == Kind.GROUP) {
				IGroup g = context.provider().instance(IGroup.class, container.domainUid);
				List<Member> members = g.getExpandedUserMembers(targetedUser.entryUid);
				members.forEach(m -> {
					DirEntry memberDE = dirService.findByEntryUid(m.uid);
					if (memberDE.email != null) {
						Map<String, String> prefs = settingService.get(memberDE.entryUid);
						String lang = prefs.get("lang");
						data.put("entity", I18nLabels.getInstance().translate(lang, container.name));
						Mailbox from = buildFrom(context, container, memberDE);
						sendMessage(from, memberDE, this.getTemplateSubject(), this.getTemplateBody(), data, lang,
								headers);
					}
				});
			} else {
				logger.info("DirEntry {} has no email", targetedUser);
			}
			return;
		}

		Mailbox from = buildFrom(context, container, targetedUser);
		Map<String, String> prefs = settingService.get(targetedUser.entryUid);
		String lang = prefs.get("lang");

		data.put("entity", I18nLabels.getInstance().translate(lang, container.name));
		sendMessage(from, targetedUser, this.getTemplateSubject(), this.getTemplateBody(), data, lang, headers);
	}

	private Mailbox buildFrom(BmContext context, ContainerDescriptor container, DirEntry de) {
		String noreply;
		if (de != null && de.email.contains("@")) {
			noreply = "no-reply@" + de.email.split("@")[1];
		} else {
			noreply = "no-reply@" + Optional.of(context.provider().instance(IDomains.class).get(container.domainUid))
					.map(d -> d.value.defaultAlias).orElse(container.domainUid);
		}
		return SendmailHelper.formatAddress(noreply, noreply);

	}

	private String buildSubject(String templateName, String locale, HashMap<String, Object> data) {
		StringWriter sw = new StringWriter();
		Template t;
		try {
			t = getTemplate(templateName, locale);
			t.process(data, sw);
		} catch (TemplateException | IOException e1) {
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
			HashMap<String, Object> data, String lang, RawField... headers) throws ServerFault {
		try {
			Mail m = new Mail();
			m.from = from;
			m.sender = from;
			m.to = SendmailHelper.formatAddress(de.displayName, de.email);
			m.subject = buildSubject(templateSubject, lang, data);

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

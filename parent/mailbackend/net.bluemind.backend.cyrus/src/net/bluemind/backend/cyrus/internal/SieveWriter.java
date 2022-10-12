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
package net.bluemind.backend.cyrus.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.internet.MimeUtility;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilter;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterContains;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterEquals;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterMatches;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName;
import net.bluemind.mailbox.service.SplittedShardsMapping;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SieveWriter {

	public enum Type {
		DOMAIN, USER, SHARED;
	}

	private static final Logger logger = LoggerFactory.getLogger(SieveWriter.class);

	private final Configuration cfg;

	public SieveWriter() {
		cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		BeansWrapper wrapper = new BeansWrapper(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		wrapper.setExposeFields(true);
		cfg.setObjectWrapper(wrapper);
		cfg.setClassForTemplateLoading(getClass(), "/templates/sieve");
	}

	public String generateSieveScript(Type type, ItemValue<Mailbox> mbox, String displayName, ItemValue<Domain> domain,
			MailFilter filter) throws IOException, TemplateException {
		Map<String, Object> data = new HashMap<>();

		data.put("vacation", verifyVacation(filter.vacation));
		data.put("vacationSubject", mailSafeEncoding(filter.vacation.subject));
		data.put("vacationText", encodeVacationText(filter.vacation));
		data.put("forward", filter.forwarding);
		data.put("domainName", domain.value.name);
		data.put("filters", getFiltersString(type, mbox, filter));
		if (type == Type.USER) {
			data.put("mailboxUid", mbox.uid);
		}

		if (type != Type.DOMAIN) {
			sieveFrom(mbox, displayName).ifPresent(from -> data.put("from", from));
			data.put("mails", expandEmails(domain, mbox.value.emails));
		}

		return applyTemplate(type, data);
	}

	public Optional<String> sieveFrom(ItemValue<Mailbox> mbox, String displayName) {
		if (mbox.value.defaultEmail() == null) {
			return Optional.empty();
		}
		String defaultEmail = mbox.value.defaultEmail().address;
		String from = defaultEmail;
		if (displayName != null && !displayName.trim().isEmpty()) {
			try {
				from = String.format("\\\"%s\\\" <%s>", MimeUtility.encodeWord(displayName), defaultEmail);
			} catch (UnsupportedEncodingException e) {
				logger.error("Unable to encode display name '{}' in vacation sieve, fallback to address only",
						displayName, e);
			}
		}
		return Optional.of(from);
	}

	private Collection<String> expandEmails(ItemValue<Domain> domain, Collection<Email> emails) {
		Collection<String> expendedEmails = new ArrayList<>();
		emails.forEach(email -> {
			if (!email.allAliases) {
				expendedEmails.add(email.toString());
				return;
			}

			String leftPart = email.toString().split("@")[0];
			expendedEmails.add(leftPart + "@" + domain.value.name);
			domain.value.aliases.forEach(alias -> expendedEmails.add(leftPart + "@" + alias));
		});

		return expendedEmails;
	}

	private List<SieveRule> getFiltersString(Type type, ItemValue<Mailbox> mbox, MailFilter filter) {
		List<SieveRule> filtersWrite = new ArrayList<>(filter.rules.size());
		for (MailFilterRule rule : filter.rules) {
			if (rule.active) {
				rule.move().ifPresent(moveAction -> {
					String deliver = moveAction.folder();
					if (!StringUtils.isNotBlank(deliver)) {
						return;
					}
					if (type == Type.SHARED) {
						String name = mbox.value.name;
						if (deliver.startsWith(name + "/")) {
							deliver = deliver.substring(name.length() + 1);
						}
						deliver = "Dossiers partagés/" + name + "/" + deliver;
					}
					moveAction.folder = UTF7Converter.encode(deliver);
				});
				filtersWrite.add(new SieveRule(rule, sieveConditions(rule)));
			}
		}
		return filtersWrite;
	}

	private String applyTemplate(Type type, Map<String, Object> data) throws IOException, TemplateException {
		StringWriter sw = new StringWriter();
		Template template = getTemplate(type);
		template.process(data, sw);
		String script = sw.toString();

		if (logger.isDebugEnabled()) {
			logger.debug("generated sieve script \n{}\n", script);
		}
		return script;
	}

	private Template getTemplate(Type type) throws IOException {
		switch (type) {
		case USER:
			return cfg.getTemplate("default.ftl");
		case SHARED:
			return cfg.getTemplate("mailshare_default.ftl");
		case DOMAIN:
			return cfg.getTemplate("domain_default.ftl");
		default:
			throw new ServerFault("Invalid type " + type);
		}
	}

	private Vacation verifyVacation(MailFilter.Vacation vac) {
		if (vac == null) {
			vac = new MailFilter.Vacation();
		}
		if (vac.enabled) {
			logger.info("Enabling sieve vacation");

			// scheduled
			long now = new Date().getTime();
			if (vac.start != null) {
				// and in range
				if (vac.start.getTime() <= now && (vac.end == null || vac.end.getTime() > now)) {
					// we are scheduled & in range, so we are enabled
				} else {
					// we are scheduled & out of range => disable
					vac = new MailFilter.Vacation();
					vac.enabled = false;
				}
			} else if (vac.end != null && vac.end.getTime() <= now) {
				vac = new MailFilter.Vacation();
				vac.enabled = false;
			}
		}

		return vac;
	}

	private String encodeVacationText(Vacation vacation) {
		if (Strings.isNullOrEmpty(vacation.textHtml)) {
			return "\"" + escapeVacationContent(vacation.text) + "\"";
		}

		MultipartVacationMessage msg = new MultipartVacationMessage(vacation.textHtml, vacation.text,
				StandardCharsets.UTF_8);

		String encodedText;
		try {
			encodedText = ":mime \"" + escapeVacationContent(msg.getContent()) + "\"";
		} catch (MimeException | IOException e) {
			encodedText = "\"" + escapeVacationContent(vacation.text) + "\"";
		}
		return encodedText;
	}

	private String mailSafeEncoding(String text) {
		if (text == null || text.trim().isEmpty()) {
			return "";
		}
		try {
			return escapeVacationContent(MimeUtility.encodeWord(text));
		} catch (UnsupportedEncodingException e) {
			logger.error("Fail to encode text content while writing sieve: {}", text, e);
			return text;
		}
	}

	private String escapeVacationContent(String text) {
		return (text != null) ? text.replace("\"", "\\\"").replace("'", "\\'") : "";
	}

	private String sieveConditions(MailFilterRule rule) {
		return rule.conditions.isEmpty() //
				? "if allof (true) {"
				: rule.conditions.stream().map(this::sieveCondition)
						.collect(Collectors.joining(",\n\t", "if allof (", ") {"));
	}

	private String sieveCondition(MailFilterRuleCondition condition) {
		if (condition.filter == null) {
			return "true";
		}

		String not = (condition.negate) ? "not " : "";
		return not + this.sieveFilter(condition.filter);
	}

	private String sieveFilter(MailFilterRuleFilter filter) {
		if (filter.operator == MailFilterRuleOperatorName.EXISTS) {
			return filter.fields.stream() //
					.map(field -> "exists [\"" + field.split("\\.")[1] + "\"]") //
					.collect(Collectors.joining(", ", "anyof (", ")"));
		}

		String type;
		String value;
		if (filter.operator == MailFilterRuleOperatorName.EQUALS) {
			List<String> values = ((MailFilterRuleFilterEquals) filter).values;
			value = escapeFilterValue(values);
			type = ":is";
		} else if (filter.operator == MailFilterRuleOperatorName.CONTAINS) {
			List<String> values = ((MailFilterRuleFilterContains) filter).values;
			value = escapeFilterValue(values);
			type = ":contains";
		} else if (filter.operator == MailFilterRuleOperatorName.MATCHES) {
			List<String> values = ((MailFilterRuleFilterMatches) filter).values;
			value = escapeFilterValue(values);
			type = ":matches";
		} else {
			value = null;
			type = "";
		}

		if (value == null) {
			return "false";
		} else {
			return filter.fields.stream() //
					.map(field -> sieveFieldFilter(field, type, value)) //
					.collect(Collectors.joining(", ", "anyof (", ")"));
		}
	}

	private String escapeFilterValue(List<String> values) {
		return values != null && !values.isEmpty() //
				? values.get(0).replace("\\", "\\\\").replaceAll("\\\"", "\\\\\"")
				: null;
	}

	private String sieveFieldFilter(String field, String type, String value) {
		if (field.startsWith("from")) {
			return "address " + type + " \"from\" \"" + value + "\"";
		} else if (field.startsWith("to")) {
			return "address " + type + " \"to\" \"" + value + "\"";
		} else if (field.startsWith("cc")) {
			return "address " + type + " \"cc\" \"" + value + "\"";
		} else if (field.equals("subject")) {
			return "header " + type + " \"Subject\" \"" + value + "\"";
		} else if (field.equals("part.content")) {
			return "body :content \"text\" " + type + " \"" + value + "\", " //
					+ "body :content \"text\" " + type + " \"" + StringEscapeUtils.escapeHtml4(value) + "\"";
		} else {
			String header = field.split(".")[1];
			return "allof (exists \"" + header + "\", header " + type + " \"" + header + "\" \"" + value + "\")";
		}
	}

	public void write(ItemValue<Mailbox> mailboxItem, String displayName, ItemValue<Domain> domain, MailFilter filter) {
		write(mailboxItem, displayName, domain, filter, 10);
	}

	/**
	 * @param mailboxItem
	 * @param domain
	 * @param filter
	 * @throws ServerFault
	 */
	private void write(ItemValue<Mailbox> mailboxItem, String displayName, ItemValue<Domain> domain, MailFilter filter,
			int count) {

		SieveClient.SieveConnectionData clientConnectionData = getSieveConnectionData(mailboxItem, domain.uid);
		Type type = mailboxItem.value.type == Mailbox.Type.user ? Type.USER : Type.SHARED;

		try (SieveClient sieveClient = new SieveClient(clientConnectionData)) {
			if (sieveClient.login()) {
				String scriptName = mailboxItem.uid + ".sieve";
				String content = generateSieveScript(type, mailboxItem, displayName, domain, filter);

				boolean res = sieveClient.putscript(scriptName, new ByteArrayInputStream(content.getBytes()));
				if (!res) {
					throw new ServerFault("Fail to put script");
				}

				res = sieveClient.activate(scriptName);
				if (!res) {
					throw new ServerFault("Fail to activate script");
				}

				if (type == Type.SHARED) {
					storeShareAnnotation(mailboxItem, domain, scriptName, clientConnectionData);
				}
			} else {
				logger.error("Fail to login to sieve as: '{}'", clientConnectionData);
				throw new ServerFault("Fail to login to sieve as '" + clientConnectionData + "'");
			}
		} catch (Exception e) {
			// BM-13462
			if (count == 0) {
				if (e instanceof ServerFault) {
					throw (ServerFault) e;
				}
				throw new ServerFault(e);
			}
			logger.info("Cannot write filter {} times. Retrying...", count);
			write(mailboxItem, displayName, domain, filter, --count);
		}

	}

	private void storeShareAnnotation(ItemValue<Mailbox> mailbox, ItemValue<Domain> domain, String scriptName,
			SieveConnectionData sieveConnectionData) throws Exception {
		try (StoreClient storeClient = new StoreClient(sieveConnectionData.host, 1143, sieveConnectionData.login,
				sieveConnectionData.password)) {
			if (storeClient.login() && !storeClient.setMailboxAnnotation(mailbox.value.name + "@" + domain.uid,
					"/vendor/cmu/cyrus-imapd/sieve", ImmutableMap.of("value.shared", scriptName))) {
				String errorMsg = String.format(
						"Unable to set IMAP annotation /vendor/cmu/cyrus-imapd/sieve on mailbox: %s@%s",
						mailbox.value.name, domain.value.name);
				logger.error(errorMsg);
				throw new ServerFault(errorMsg);
			}
		}
	}

	private SieveConnectionData getSieveConnectionData(ItemValue<Mailbox> mailbox, String domain) {
		SieveConnectionData connectionData = null;
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		if (mailbox.value.type == Mailbox.Type.user) {
			// mailboxuid == userUid
			ItemValue<User> user = userService.getComplete(mailbox.uid);
			String login = user.value.login + "@" + domain;
			ItemValue<Server> server = SplittedShardsMapping
					.remap(serverService.getComplete(mailbox.value.dataLocation));
			connectionData = new SieveConnectionData(login, "admin0", Token.admin0(), server.value.ip);
		} else {
			String login = "bmhiddensysadmin@" + domain;
			ItemValue<Server> server = serverService.getComplete(mailbox.value.dataLocation);
			String host = server.value.ip;
			final Sudo sudo = Sudo.forLogin("bmhiddensysadmin", domain);
			connectionData = new SieveConnectionData(login, sudo.context.getSessionId(), host) {

				@Override
				public void close() throws IOException {
					sudo.close();
				}

			};
		}
		return connectionData;
	}

	/**
	 * @param domainUid
	 * @param filter
	 * @throws ServerFault
	 */
	public void write(String domainUid, MailFilter filter) {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		List<Assignment> assignments = serverService.getAssignments(domainUid);

		List<ItemValue<Server>> servers = new ArrayList<>(assignments.size());
		for (Assignment assignment : assignments) {
			if (assignment.tag.equals("mail/imap")) {
				ItemValue<Server> s = serverService.getComplete(assignment.serverUid);
				if (s == null) {
					throw new ServerFault(String.format("Unknown server UID %s assigned as mail/imap to domain %s",
							assignment.serverUid, domainUid));
				}

				servers.add(s);
			}
		}

		try (Sudo sudo = Sudo.forLogin("bmhiddensysadmin", domainUid)) {
			for (ItemValue<Server> server : servers) {
				SieveConnectionData sieveConnectionData = new SieveConnectionData("bmhiddensysadmin@" + domainUid,
						sudo.context.getSessionId(), server.value.ip);
				try (SieveClient sieveClient = new SieveClient(sieveConnectionData)) {
					if (sieveClient.login()) {
						String scriptName = domainUid + ".sieve";
						String content = generateSieveScript(Type.DOMAIN, null, null,
								ItemValue.create(domainUid,
										Domain.create(domainUid, domainUid, scriptName, Collections.emptySet())),
								filter);

						boolean res = sieveClient.putscript(scriptName, new ByteArrayInputStream(content.getBytes()));
						if (!res) {
							throw new ServerFault("Fail to put script");
						}

						res = sieveClient.activate(scriptName);
						if (!res) {
							throw new ServerFault("Fail to activate script");
						}

					} else {
						logger.error("Fail to login to sieve. Login: 'admin0', server: {}", server.value.ip);
						throw new ServerFault("Fail to login to sieve (admin0) on server " + server.value.ip);
					}
				} catch (ServerFault sf) {
					throw sf;
				} catch (Exception e) {
					logger.error("Fail to write sieve for domain {}, server {}", domainUid, server.value.ip);
					throw new ServerFault(e);
				}
			}
		}
	}
}

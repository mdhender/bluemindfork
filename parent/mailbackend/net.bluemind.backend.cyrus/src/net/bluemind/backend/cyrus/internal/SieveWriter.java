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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.backend.cyrus.Sudo;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SieveWriter {

	public static enum Type {
		Domain, User, Shared;
	}

	private static final Logger logger = LoggerFactory.getLogger(SieveWriter.class);

	private final Configuration cfg;

	public SieveWriter() {
		cfg = new Configuration();
		BeansWrapper wrapper = new BeansWrapper();
		wrapper.setExposeFields(true);
		cfg.setObjectWrapper(wrapper);
		cfg.setClassForTemplateLoading(getClass(), "/templates/sieve");
	}

	public String generateSieveScript(Type type, ItemValue<Mailbox> mbox, ItemValue<Domain> domain, MailFilter filter)
			throws IOException, TemplateException, ServerFault {
		Map<String, Object> data = new HashMap<>();

		data.put("vacation", verifyVacation(filter.vacation));
		data.put("forward", filter.forwarding);
		data.put("domainName", domain.value.name);
		data.put("filters", getFiltersString(type, mbox, filter));
		if (type == Type.User) {
			data.put("mailboxUid", mbox.uid);
		}

		if (type != Type.Domain) {
			Email defaultEmail = mbox.value.defaultEmail();
			if (defaultEmail != null) {
				data.put("from", defaultEmail.address);
			}
			data.put("mails", expandEmails(domain, mbox.value.emails));
		}

		return applyTemplate(type, data);
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
			domain.value.aliases.forEach(alias -> {
				expendedEmails.add(leftPart + "@" + alias);
			});
		});

		return expendedEmails;
	}

	private List<SieveRule> getFiltersString(Type type, ItemValue<Mailbox> mbox, MailFilter filter) {
		List<SieveRule> filtersWrite = new ArrayList<>(filter.rules.size());
		for (MailFilter.Rule f : filter.rules) {
			if (f.active) {
				String criteria = f.criteria;
				if (Strings.isNullOrEmpty(criteria)) {
					// COAX-8, l’import des règles de Zimbra vers Bluemind.
					// Certains
					// filtres avaient une action de défini mais pas de critères
					logger.warn("empty criteria in sieve filter rule!");
					continue;
				}

				String deliver = f.deliver;
				if (f.deliver != null && StringUtils.isNotBlank(deliver.trim())) {
					if (type == Type.Shared) {
						String name = mbox.value.name;
						if (deliver.startsWith(name + "/")) {
							deliver = deliver.substring(name.length() + 1);
						}
						deliver = "Dossiers partagés/" + name + "/" + deliver;
					}
					f.deliver = UTF7Converter.encode(deliver);
				}
				filtersWrite.add(new SieveRule(f, getRule(f.criteria)));
			}
		}
		return filtersWrite;
	}

	private String applyTemplate(Type type, Map<String, Object> data)
			throws IOException, ServerFault, TemplateException {
		StringWriter sw = new StringWriter();
		Template template = getTemplate(type);
		template.process(data, sw);
		String script = sw.toString();

		if (logger.isDebugEnabled()) {
			logger.debug("generated sieve script \n{}\n", script);
		}
		return script;
	}

	private Template getTemplate(Type type) throws IOException, ServerFault {
		switch (type) {
		case User:
			return cfg.getTemplate("default.ftl");
		case Shared:
			return cfg.getTemplate("mailshare_default.ftl");
		case Domain:
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
			if (vac.start != null) {
				long now = new Date().getTime();
				Long start = new BmDateTimeWrapper(vac.start).toUTCTimestamp();
				Long end = null;
				if (vac.end != null) {
					end = new BmDateTimeWrapper(vac.end).toUTCTimestamp();
				}
				// and in range
				if (start <= now && (end == null || end > now)) {
					// we are scheduled & in range, so we are enabled
				} else {
					// we are scheduled & out of range => disable
					vac = new MailFilter.Vacation();
					vac.enabled = false;
				}
			}
		}

		if (vac.subject != null && !vac.subject.trim().isEmpty()) {
			vac.subject = "=?UTF-8?b?" + Base64.getEncoder().encodeToString(vac.subject.getBytes()) + "?=";
		}

		return vac;
	}

	private String getRule(String criteria) {
		StringBuilder sb = new StringBuilder();
		String[] crits = criteria.split("\n");
		String crit;
		for (int i = 0; i < crits.length; i++) {
			String c = crits[i];
			if (i == 0) {
				sb.append("if allof (");
			} else {
				sb.append(",\n\t");
			}
			crit = appendCriteria(c);
			if (crit != null) {
				sb.append(crit);
			}
		}
		sb.append(") {");
		return sb.toString();
	}

	private String appendCriteria(String c) {
		if (c.equals("MATCHALL")) {
			return "true";
		}

		int i = c.indexOf(':');
		String crit = c.substring(0, i);
		c = c.substring(i + 1);
		i = c.indexOf(": ");
		String matchType = c.substring(0, i);
		String value = c.substring(i + 2);
		// backslash \
		value = value.replace("\\", "\\\\");
		// backslash "
		value = value.replaceAll("\\\"", "\\\\\"");

		String not = "";
		String type = "";
		if (matchType.equals("EXISTS")) {
			return "exists [\"" + crit + "\"]";
		} else if (matchType.equals("DOESNOTEXIST")) {
			return "not exists [\"" + crit + "\"]";
		} else if (matchType.equals("IS")) {
			not = "";
			type = ":is";
		} else if (matchType.equals("ISNOT")) {
			not = "not";
			type = ":is";
		} else if (matchType.equals("CONTAINS")) {
			not = "";
			type = ":contains";
		} else if (matchType.equals("DOESNOTCONTAIN")) {
			not = "not";
			type = ":contains";
		} else if (matchType.equals("MATCHES")) {
			not = "";
			type = ":matches";
		} else if (matchType.equals("DOESNOTMATCH")) {
			not = "not";
			type = ":matches";
		}

		if (crit.equals("FROM")) {
			return not + " address " + type + " \"from\" \"" + value + "\"";
		} else if (crit.equals("TO")) {
			return not + " address " + type + " [\"to\", \"cc\"] \"" + value + "\"";
		} else if (crit.equals("SUBJECT")) {
			return not + " header " + type + " \"Subject\" \"" + value + "\"";
		} else if (crit.equals("BODY")) {
			return "anyof (" + not + " body :content \"text\" " + type + " \"" + value + "\", " + not
					+ " body :content \"text\" " + type + " \"" + StringEscapeUtils.escapeHtml(value) + "\")";
		} else {
			if (!not.isEmpty()) {
				return "exists \"" + crit + "\",\n\t" + not + " header " + type + " \"" + crit + "\" \"" + value + "\"";
			}

			return "header " + type + " \"" + crit + "\" \"" + value + "\"";
		}
	}

	public void write(ItemValue<Mailbox> mailbox, ItemValue<Domain> domain, MailFilter filter) throws ServerFault {
		write(mailbox, domain, filter, 10);
	}

	/**
	 * @param mailbox
	 * @param domain
	 * @param filter
	 * @throws ServerFault
	 */
	private void write(ItemValue<Mailbox> mailbox, ItemValue<Domain> domain, MailFilter filter, int count)
			throws ServerFault {

		SieveClient.SieveConnectionData clientConnectionData = getSieveConnectionData(mailbox, domain.uid);
		Type type = mailbox.value.type == Mailbox.Type.user ? Type.User : Type.Shared;

		try (SieveClient sieveClient = new SieveClient(clientConnectionData)) {
			if (sieveClient.login()) {
				String scriptName = mailbox.uid + ".sieve";
				String content = generateSieveScript(type, mailbox, domain, filter);

				boolean res = sieveClient.putscript(scriptName, new ByteArrayInputStream(content.getBytes()));
				if (!res) {
					throw new ServerFault("Fail to put script");
				}

				res = sieveClient.activate(scriptName);
				if (!res) {
					throw new ServerFault("Fail to activate script");
				}

				if (type == Type.Shared) {
					storeShareAnnotation(mailbox, domain, scriptName, clientConnectionData);
				}
			} else {
				logger.error("Fail to login to sieve. Login: '{}'", clientConnectionData.login);
				throw new ServerFault("Fail to login to sieve. Login '" + clientConnectionData.login + "'");
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
			write(mailbox, domain, filter, --count);
		}

	}

	private void storeShareAnnotation(ItemValue<Mailbox> mailbox, ItemValue<Domain> domain, String scriptName,
			SieveConnectionData sieveConnectionData) throws IMAPException, Exception {
		try (StoreClient storeClient = new StoreClient(sieveConnectionData.host, 1143, sieveConnectionData.login,
				sieveConnectionData.password)) {
			if (storeClient.login() && !storeClient.setAnnotation("\"" + mailbox.value.name + "@" + domain.value.name
					+ "\" \"/vendor/cmu/cyrus-imapd/sieve\" (\"value.shared\" \"" + scriptName + "\")")) {
				String errorMsg = String.format(
						"Unable to set IMAP annotation /vendor/cmu/cyrus-imapd/sieve on mailbox: %s@%s",
						mailbox.value.name, domain.value.name);
				logger.error(errorMsg);
				throw new ServerFault(errorMsg);
			}
		}
	}

	private SieveConnectionData getSieveConnectionData(ItemValue<Mailbox> mailbox, String domain) throws ServerFault {
		SieveConnectionData connectionData = null;
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		if (mailbox.value.type == Mailbox.Type.user) {
			// mailboxuid == userUid
			ItemValue<User> user = userService.getComplete(mailbox.uid);
			String login = user.value.login + "@" + domain;
			ItemValue<Server> server = serverService.getComplete(mailbox.value.dataLocation);
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
	public void write(String domainUid, MailFilter filter) throws ServerFault {
		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		List<Assignment> assignments = serverService.getAssignments(domainUid);

		List<ItemValue<Server>> servers = new ArrayList<ItemValue<Server>>(assignments.size());
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
						String content = generateSieveScript(Type.Domain, null,
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

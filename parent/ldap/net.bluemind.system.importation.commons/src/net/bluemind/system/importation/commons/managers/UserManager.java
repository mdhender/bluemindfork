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
package net.bluemind.system.importation.commons.managers;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.enhancer.UserData;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.tools.VCardHelper;
import net.bluemind.user.api.User;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public abstract class UserManager extends EntityManager {
	private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

	private static final Pattern LOGIN_CHAR_TO_REMOVE = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private static final Pattern LOGIN_CHAR_TO_REPLACE = Pattern.compile("[^a-z0-9-._]");

	public boolean create = true;
	public final Entry entry;

	public ItemValue<User> user = ItemValue.create(Item.create(null, null), getEmptyUser());
	public byte[] userPhoto = null;

	private boolean mailFilterUpdated = false;
	private MailFilter mailFilter = new MailFilter();

	public final Optional<Set<UuidMapper>> splitGroupMembers;
	public Integer mailboxQuota = null;

	public UserManager(ItemValue<Domain> domain, Entry entry) {
		super(domain);
		this.entry = entry;
		this.splitGroupMembers = Optional.empty();
	}

	public UserManager(ItemValue<Domain> domain, Entry entry, Optional<Set<UuidMapper>> splitGroupMembers) {
		super(domain);
		this.entry = entry;

		if (splitGroupMembers.isPresent()) {
			this.splitGroupMembers = Optional.of(Collections.unmodifiableSet(splitGroupMembers.get()));
		} else {
			this.splitGroupMembers = splitGroupMembers;
		}
	}

	public abstract String getExternalId(IImportLogger importLogger);

	protected abstract void setLoginFromDefaultAttribute(IImportLogger importLogger)
			throws LdapInvalidAttributeValueException;

	protected abstract void manageArchived() throws LdapInvalidAttributeValueException;

	protected abstract void setMailRouting();

	protected abstract List<String> getEmails();

	protected abstract Parameters getDirectoryParameters();

	protected abstract List<IEntityEnhancer> getEntityEnhancerHooks();

	protected abstract void manageContactInfos() throws LdapInvalidAttributeValueException;

	protected abstract void manageQuota(IImportLogger importLogger) throws LdapInvalidAttributeValueException;

	public abstract List<? extends UuidMapper> getUserGroupsMemberGuid(LdapConnection ldapCon);

	public void update(ItemValue<User> currentUser, MailFilter mailFilter) throws ServerFault {
		update(new ImportLogger(), currentUser, mailFilter);
	}

	public void update(IImportLogger importLogger, ItemValue<User> currentUser, MailFilter mailFilter) {
		if (currentUser != null) {
			user = currentUser;
			if (user.value.contactInfos == null) {
				user.value.contactInfos = new VCard();
			}

			if (mailFilter != null) {
				this.mailFilter = mailFilter;
			}
			create = false;
		}

		doUpdate(importLogger);
	}

	private void doUpdate(IImportLogger importLogger) {
		try {
			if (create) {
				user.uid = UIDGenerator.uid();
				user.externalId = getExternalId(importLogger);
			}

			setLogin(importLogger);

			user.value.password = null;
			manageArchived();

			manageContactInfos();

			setMailRouting();
			manageEmails(getEmails());
			manageQuota(importLogger);

			UserData pluginUser = new UserData() {
				@Override
				public String getUid() {
					return UserManager.this.user.uid;
				}
			};

			pluginUser.user = user.value;
			pluginUser.photo = userPhoto;
			pluginUser.mailboxQuota = mailboxQuota;
			pluginUser.mailFilter = MailFilter.copy(getMailFilter());
			for (IEntityEnhancer iee : getEntityEnhancerHooks()) {
				pluginUser = iee.enhanceUser(importLogger.withoutStatus(), getDirectoryParameters(), domain, entry,
						pluginUser);
			}

			user.value = pluginUser.user;
			userPhoto = pluginUser.photo;
			mailboxQuota = pluginUser.mailboxQuota;
			setMailFilter(pluginUser.mailFilter);
		} catch (LdapInvalidAttributeValueException e) {
			String errorMsg = String.format("Unable to convert entry:%s to a valid BlueMind user", entry.getDn());
			logger.error(errorMsg);
			throw new ServerFault(errorMsg);
		}
	}

	private void setLogin(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
		Optional<String> userLogin = Optional.empty();
		for (IEntityEnhancer iee : getEntityEnhancerHooks()) {
			userLogin = iee.getUserLogin(importLogger.withoutStatus(), getDirectoryParameters(), domain, entry);
		}

		if (userLogin.isPresent() && !userLogin.get().trim().isEmpty()) {
			user.value.login = normalizeLogin(userLogin.get().trim());
			return;
		}

		setLoginFromDefaultAttribute(importLogger);
	}

	private static User getEmptyUser() {
		User user = new User();
		user.contactInfos = new VCard();
		return user;
	}

	protected void manageEmails(List<String> userEmails) {
		List<Email> emails = new LinkedList<>();

		Map<String, Set<String>> localEmails = userEmails.stream().filter(userEmail -> isLocalEmail(userEmail))
				.collect(Collectors.toMap(this::getEmailLeftPart, this::getEmailRightParts, this::mergeEmailRightParts,
						HashMap::new));
		if (!localEmails.isEmpty()) {
			String defaultLocalEmail = getDefaultLocalEmail(userEmails);
			addImplicitLocalEmail(localEmails);

			Set<String> domainAliases = getDomainAliases();

			localEmails.entrySet().stream()
					.filter(localEmailEntry -> localEmailEntry.getValue().size() == domainAliases.size())
					.forEach(localEmailEntry -> emails.add(Email.create(
							defaultLocalEmail.startsWith(localEmailEntry.getKey() + "@") ? defaultLocalEmail
									: localEmailEntry.getKey() + "@" + domain.value.name,
							defaultLocalEmail.startsWith(localEmailEntry.getKey() + "@"), true)));

			localEmails.entrySet().stream()
					.filter(localEmailEntry -> localEmailEntry.getValue().size() != domainAliases.size()).forEach(
							localEmailEntry -> localEmailEntry.getValue()
									.forEach(domain -> emails.add(Email.create(localEmailEntry.getKey() + "@" + domain,
											defaultLocalEmail.equals(localEmailEntry.getKey() + "@" + domain),
											false))));
		} else {
			Set<String> extEmails = userEmails.stream().filter(userEmail -> !isLocalEmail(userEmail))
					.collect(Collectors.toSet());
			if (!extEmails.isEmpty()) {
				String defaultExtEmail = userEmails.stream().filter(userEmail -> !isLocalEmail(userEmail)).findFirst()
						.orElse(extEmails.iterator().next());

				extEmails.forEach(email -> emails.add(Email.create(email, email.equals(defaultExtEmail))));
			}

			setNoMailRouting();
		}

		if (logger.isDebugEnabled()) {
			emails.stream().forEach(e -> logger.debug(e.address + " def:" + e.isDefault + " allalias:" + e.allAliases));
		}

		user.value.emails = emails;

		user.value.contactInfos.communications.emails = VCardHelper.manageEmails(emails);
	}

	private void addImplicitLocalEmail(Map<String, Set<String>> localEmails) {
		if (!localEmails.containsKey(user.value.login)) {
			localEmails.put(user.value.login, new HashSet<>(Arrays.asList(domain.value.name)));
		} else {
			localEmails.get(user.value.login).add(domain.value.name);
		}
	}

	private void setNoMailRouting() {
		user.value.routing = Routing.none;
		disableVacationAndForwarding();
	}

	protected void setExternalMailRouting() {
		user.value.routing = Routing.external;
		disableVacationAndForwarding();
	}

	private void disableVacationAndForwarding() {
		if (mailFilter.vacation.enabled) {
			mailFilter.vacation.enabled = false;
			mailFilterUpdated = true;
		}

		if (mailFilter.forwarding.enabled) {
			mailFilter.forwarding.enabled = false;
			mailFilterUpdated = true;
		}

		if (mailFilter.forwarding.localCopy) {
			mailFilter.forwarding.localCopy = false;
			mailFilterUpdated = true;
		}
	}

	protected MailFilter getMailFilter() {
		return mailFilter;
	}

	protected void setMailFilter(MailFilter mailFilter) {
		if (mailFilter == null) {
			return;
		}

		if (!this.mailFilter.equals(mailFilter)) {
			mailFilterUpdated = true;
			this.mailFilter = mailFilter;
		}
	}

	public Optional<MailFilter> getUpdatedMailFilter() {
		if (create || mailFilterUpdated) {
			return Optional.of(mailFilter);
		}

		return Optional.empty();
	}

	protected String normalizeLogin(String login) {
		String temp = Normalizer.normalize(login, Normalizer.Form.NFD);
		String normalizedLogin = LOGIN_CHAR_TO_REMOVE.matcher(temp).replaceAll("").toLowerCase();

		return LOGIN_CHAR_TO_REPLACE.matcher(normalizedLogin).replaceAll("_");
	}
}
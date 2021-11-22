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
package net.bluemind.system.ldap.importation.internal.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard.DeliveryAddressing;
import net.bluemind.addressbook.api.VCard.Identification.FormatedName;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.importation.tools.VCardHelper;
import net.bluemind.system.ldap.importation.Activator;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.search.MemberOfLdapSearch;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 */
public class UserManagerImpl extends UserManager {
	private static final Logger logger = LoggerFactory.getLogger(UserManagerImpl.class);

	public static final String LDAP_LOGIN = "uid";
	private static final String LDAP_DISPLAYNAME = "displayName";
	private static final String LDAP_FIRSTNAME = "givenName";
	private static final String LDAP_LASTNAME = "sn";
	private static final String LDAP_DESCRIPTION = "description";
	private static final String LDAP_PHOTO = "jpegPhoto";
	public static final String LDAP_MEMBER_OF = "memberOf";
	private static final String[] LDAP_MAIL = { "mail", "mailLocalAddress", "mailAlternateAddress",
			"gosaMailAlternateAddress" };
	private static final String[] LDAP_MAIL_QUOTA = { "mailQuotaSize", "mailQuota", "gosaMailQuota" };
	private static final String LDAP_JOBTITLE = "title";
	private static final String LDAP_ORGANIZATION = "o";
	private static final String LDAP_ORGANIZATIONUNIT = "ou";
	private static final String LDAP_DEPARTMENTNUMBER = "departmentNumber";

	// Address
	private static final String LDAP_LOCALITY = "l";
	private static final String LDAP_POSTALCODE = "postalCode";
	private static final String LDAP_POSTOFFICEBOX = "postOfficeBox";
	private static final String LDAP_POSTALADDRESS = "postalAddress";
	private static final String LDAP_COUNTRYNAME = "st";

	// Phones
	private static final String[] BM_PHONE_WORK_LABEL = { "voice", "work" };
	private static final String[] LDAP_PHONE_WORK = { "telephoneNumber" };
	private static final String[] BM_PHONE_HOME_LABEL = { "voice", "home" };
	private static final String[] LDAP_PHONE_HOME = { "homePhone" };
	private static final String[] BM_PHONE_MOBILE_LABEL = { "cell", "voice" };
	private static final String[] LDAP_PHONE_MOBILE = { "mobile" };
	private static final String[] BM_PHONE_FAX_LABEL = { "fax", "work" };
	private static final String[] LDAP_PHONE_FAX = { "facsimileTelephoneNumber" };
	private static final String[] BM_PAGER_LABEL = { "pager", "voice" };
	private static final String[] LDAP_PAGER = { "pager" };

	private final LdapParameters ldapParameters;
	private final Optional<Set<UuidMapper>> splitGroupMembers;

	public static Optional<UserManager> build(LdapParameters ldapParameters, ItemValue<Domain> domain, Entry entry,
			Optional<Set<UuidMapper>> splitGroupMembers) {
		return Optional.of(new UserManagerImpl(ldapParameters, domain, entry, splitGroupMembers));
	}

	public static Optional<UserManager> build(LdapParameters ldapParameters, ItemValue<Domain> domain, Entry entry) {
		return Optional.of(new UserManagerImpl(ldapParameters, domain, entry, Optional.empty()));
	}

	/**
	 * @param create
	 * @param domainAliases
	 * @param splitGroupMembers
	 * @param user
	 * @param mailFilter
	 * @param splitGroupMembers
	 */
	private UserManagerImpl(LdapParameters ldapParameters, ItemValue<Domain> domain, Entry entry,
			Optional<Set<UuidMapper>> splitGroupMembers) {
		super(domain, entry);
		this.ldapParameters = ldapParameters;
		this.splitGroupMembers = splitGroupMembers;
	}

	@Override
	public String getExternalId(IImportLogger importLogger) {
		return LdapConstants.EXTID_PREFIX
				+ LdapHelper.checkMandatoryAttribute(importLogger, entry, ldapParameters.ldapDirectory.extIdAttribute);
	}

	@Override
	protected void setLoginFromDefaultAttribute(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
		if (!entry.containsAttribute(LDAP_LOGIN)) {
			if (importLogger != null) {
				HashMap<String, String> messages = new HashMap<>(2);
				messages.put("en",
						String.format("Unable to manage user: %s, missing attribute: %s", entry.getDn(), LDAP_LOGIN));
				messages.put("fr", String.format("Impossible de gérer l'utilisateur: %s, attribut manquant: %s",
						entry.getDn(), LDAP_LOGIN));

				importLogger.error(messages);
			}

			throw new ServerFault(
					String.format("Unable to manage user: %s, missing attribute: %s", entry.getDn(), LDAP_LOGIN));
		}

		user.value.login = normalizeLogin(LdapHelper.checkMandatoryAttribute(importLogger, entry, LDAP_LOGIN));
	}

	@Override
	protected void manageArchived() throws LdapInvalidAttributeValueException {
		user.value.archived = false;
	}

	@Override
	protected void setMailRouting() {
		user.value.routing = Routing.internal;

		if (!ldapParameters.splitDomain.splitRelayEnabled) {
			return;
		}

		if (splitGroupMembers.isPresent()) {
			LdapUuidMapper.fromExtId(user.externalId).ifPresent(eI -> {
				if (splitGroupMembers.get().contains(eI)) {
					setExternalMailRouting();
				}
			});
		}
	}

	@Override
	protected List<String> getEmails() {
		return getAttributesValues(entry, LDAP_MAIL);
	}

	@Override
	protected Parameters getDirectoryParameters() {
		return ldapParameters;
	}

	@Override
	protected List<IEntityEnhancer> getEntityEnhancerHooks() {
		return Activator.getEntityEnhancerHooks();
	}

	@Override
	protected void manageContactInfos() throws LdapInvalidAttributeValueException {
		user.value.contactInfos.explanatory.note = getAttributeValue(entry, LDAP_DESCRIPTION);

		if (entry.containsAttribute(LDAP_DISPLAYNAME)) {
			user.value.contactInfos.identification.formatedName = FormatedName
					.create(getAttributeValue(entry, LDAP_DISPLAYNAME));
		} else {
			user.value.contactInfos.identification.formatedName = FormatedName.create(null);
		}

		user.value.contactInfos.identification.name.givenNames = getAttributeValue(entry, LDAP_FIRSTNAME);
		user.value.contactInfos.identification.name.familyNames = getAttributeValue(entry, LDAP_LASTNAME);

		user.value.contactInfos.organizational.title = getAttributeValue(entry, LDAP_JOBTITLE);
		user.value.contactInfos.organizational.org.company = getAttributeValue(entry, LDAP_ORGANIZATION);
		user.value.contactInfos.organizational.org.division = getAttributeValue(entry, LDAP_ORGANIZATIONUNIT);
		user.value.contactInfos.organizational.org.department = getAttributeValue(entry, LDAP_DEPARTMENTNUMBER);

		if (entry.containsAttribute(LDAP_PHOTO)) {
			try {
				userPhoto = entry.get(LDAP_PHOTO).getBytes();
			} catch (LdapInvalidAttributeValueException liave) {
				logger.warn("Unable to retrieve {} for {}: {} - ignoring attribute", LDAP_PHOTO,
						entry.getDn().getName(), liave.getMessage());
			}
		}

		manageUserPhones();
		manageAddress();
	}

	private void manageAddress() {
		boolean addressSet = false;
		DeliveryAddressing userAddress = new DeliveryAddressing();

		String ldapValue = getAttributeValue(entry, LDAP_LOCALITY);
		if (ldapValue != null) {
			addressSet = true;
			userAddress.address.locality = ldapValue;
		}

		ldapValue = getAttributeValue(entry, LDAP_POSTALCODE);
		if (ldapValue != null) {
			addressSet = true;
			userAddress.address.postalCode = ldapValue;
		}

		ldapValue = getAttributeValue(entry, LDAP_COUNTRYNAME);
		if (ldapValue != null) {
			addressSet = true;
			userAddress.address.countryName = ldapValue;
		}

		ldapValue = getAttributeValue(entry, LDAP_POSTALADDRESS);
		if (ldapValue != null) {
			addressSet = true;
			userAddress.address.streetAddress = ldapValue.replaceAll("\\$", "\n");
		}

		ldapValue = getAttributeValue(entry, LDAP_POSTOFFICEBOX);
		if (ldapValue != null) {
			addressSet = true;
			userAddress.address.postOfficeBox = ldapValue;
		}

		if (addressSet) {
			userAddress.address.parameters = Arrays.asList(Parameter.create("TYPE", "work"));
			user.value.contactInfos.deliveryAddressing = Arrays.asList(userAddress);
		} else {
			user.value.contactInfos.deliveryAddressing = Collections.emptyList();
		}
	}

	@Override
	protected void manageQuota(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
		for (String quotaAttribute : LDAP_MAIL_QUOTA) {
			if (entry.containsAttribute(quotaAttribute)) {
				try {
					long userQuota = Long.parseLong(entry.get(quotaAttribute).getString().trim());
					// Must be in octet in LDAP, convert in KiB
					mailboxQuota = (int) (userQuota / 1024);
					break;
				} catch (NumberFormatException nfe) {
					logger.warn("Invalid user quota in LDAP: {}", nfe.getMessage());

					HashMap<String, String> messages = new HashMap<String, String>(2);
					messages.put("en", String.format("Invalid user quota in LDAP for user: %s - %s", entry.getDn(),
							nfe.getMessage()));
					messages.put("fr", String.format("Quota invalide dans l'annuaire LDAP pour l'utilisateur: %s - %s",
							entry.getDn(), nfe.getMessage()));
					importLogger.error(messages);
				}
			}
		}
	}

	private void manageUserPhones() {
		user.value.contactInfos.communications.tels = VCardHelper.managePhones(entry, LDAP_PHONE_HOME,
				BM_PHONE_HOME_LABEL);

		user.value.contactInfos.communications.tels
				.addAll(VCardHelper.managePhones(entry, LDAP_PHONE_WORK, BM_PHONE_WORK_LABEL));

		user.value.contactInfos.communications.tels
				.addAll(VCardHelper.managePhones(entry, LDAP_PHONE_MOBILE, BM_PHONE_MOBILE_LABEL));

		user.value.contactInfos.communications.tels
				.addAll(VCardHelper.managePhones(entry, LDAP_PHONE_FAX, BM_PHONE_FAX_LABEL));

		user.value.contactInfos.communications.tels.addAll(VCardHelper.managePhones(entry, LDAP_PAGER, BM_PAGER_LABEL));
	}

	@Override
	public List<? extends UuidMapper> getUserGroupsMemberGuid(LdapConnection ldapCon) {
		return new MemberOfLdapSearch(ldapParameters).getUserGroupsByMemberUuid(ldapCon, ldapParameters, entry);
	}
}

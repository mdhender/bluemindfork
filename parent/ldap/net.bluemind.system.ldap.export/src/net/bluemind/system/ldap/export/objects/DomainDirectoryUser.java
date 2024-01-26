/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.export.objects;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.VCard.Communications.Tel;
import net.bluemind.addressbook.api.VCard.DeliveryAddressing.Address;
import net.bluemind.addressbook.api.VCard.Parameter;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.export.Activator;
import net.bluemind.system.ldap.export.enhancer.IEntityEnhancer;
import net.bluemind.user.api.User;

public class DomainDirectoryUser extends LdapObjects {
	private static final String RDN_ATTRIBUTE = "uid";
	private static final String BMUID = "bmUid";
	private static final String PAGER = "pager";
	private static final String VOICE = "voice";
	private static final String SHADOWMAX = "shadowMax";

	private final ItemValue<Domain> domain;
	private final ItemValue<User> user;
	private final byte[] userPhoto;
	private final Optional<Integer> passwordLifetime;

	public static final List<String> ldapAttrsStringsValues = List.of( //
			"objectclass",
			// Identity
			BMUID, "bmHidden", "cn", "displayName", "sn", "employeeType", "givenName", "description", "o", "ou",
			"departmentNumber", "title", "jpegPhoto",
			// Email
			"mail",
			// Phones
			"telephoneNumber", "facsimileTelephoneNumber", "homePhone", "mobile", PAGER,
			// Address
			"l", "postalCode", "postOfficeBox", "postalAddress", "street", "st", "registeredAddress",
			// Password
			"userPassword", "shadowLastChange", SHADOWMAX);

	public DomainDirectoryUser(ItemValue<Domain> domain, Optional<Integer> passwordLifetime, ItemValue<User> user,
			byte[] userPhoto) {
		this.domain = domain;
		this.user = user;
		this.passwordLifetime = passwordLifetime;
		this.userPhoto = userPhoto;
	}

	public DomainDirectoryUser(ItemValue<Domain> domain, Optional<Integer> passwordLifetime, ItemValue<User> user) {
		this.domain = domain;
		this.passwordLifetime = passwordLifetime;
		this.user = user;
		this.userPhoto = null;
	}

	private void initPassword(Entry ldapEntry) throws LdapException {
		ldapEntry.add("userPassword", "{SASL}" + user.value.login + "@" + domain.value.name);

		if (user.value.passwordLastChange != null) {
			ldapEntry.add("shadowLastChange", Long.toString(user.value.passwordLastChange.toInstant()
					.atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()));
		}

		if (user.value.passwordMustChange) {
			ldapEntry.add(SHADOWMAX, "0");
		} else if (!user.value.passwordNeverExpires && passwordLifetime.isPresent()) {
			ldapEntry.add(SHADOWMAX, Integer.toString(passwordLifetime.get()));
		}
	}

	private void initAddress(Entry ldapEntry) throws LdapException {
		if (user.value.contactInfos.deliveryAddressing.isEmpty()) {
			return;
		}

		Address address = user.value.contactInfos.deliveryAddressing.get(0).address;

		if (!Strings.isNullOrEmpty(address.locality)) {
			ldapEntry.add("l", address.locality);
		}

		if (!Strings.isNullOrEmpty(address.postalCode)) {
			ldapEntry.add("postalCode", address.postalCode);
		}

		if (!Strings.isNullOrEmpty(address.postOfficeBox)) {
			ldapEntry.add("postOfficeBox", address.postOfficeBox);
		}

		if (!Strings.isNullOrEmpty(address.streetAddress)) {
			ldapEntry.add("postalAddress", address.streetAddress);
			ldapEntry.add("street", address.streetAddress);
		}

		if (!Strings.isNullOrEmpty(address.countryName)) {
			ldapEntry.add("st", address.countryName);
		}

		String registeredAddress = getFormatedRegisteredAddress(address);
		if (registeredAddress != null) {
			ldapEntry.add("registeredAddress", registeredAddress);
		}
	}

	private void initPhone(Entry ldapEntry) throws LdapException {
		for (Tel tel : user.value.contactInfos.communications.tels) {
			String attr = getPhoneLdapAttrName(tel.parameters);
			if (attr != null) {
				ldapEntry.add(attr, tel.value);
			}
		}
	}

	private String getPhoneLdapAttrName(List<Parameter> parameters) {
		Set<String> params = parameters.stream().filter(p -> p.label.equals("TYPE")).map(p -> p.value.toLowerCase())
				.collect(Collectors.toSet());

		if (params.contains("work") && params.contains(VOICE)) {
			return "telephoneNumber";
		} else if (params.contains("work") && params.contains("fax")) {
			return "facsimileTelephoneNumber";
		} else if (params.contains("home") && params.contains(VOICE)) {
			return "homePhone";
		} else if (params.contains("cell") && params.contains(VOICE)) {
			return "mobile";
		} else if (params.contains(PAGER) && params.contains(VOICE)) {
			return PAGER;
		}

		return null;
	}

	private void initEmail(Entry ldapEntry) throws LdapException {
		if (!Strings.isNullOrEmpty(user.value.contactInfos.defaultMail())) {
			ldapEntry.add("mail", user.value.contactInfos.defaultMail());
		}
	}

	private void initIdentity(Entry ldapEntry) throws LdapException {
		if (!Strings.isNullOrEmpty(user.value.contactInfos.identification.formatedName.value)) {
			ldapEntry.add("cn", user.value.contactInfos.identification.formatedName.value);
			ldapEntry.add("displayName", user.value.contactInfos.identification.formatedName.value);
		} else {
			ldapEntry.add("cn", user.value.login);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.identification.name.familyNames)) {
			ldapEntry.add("sn", user.value.contactInfos.identification.name.familyNames);
		} else {
			ldapEntry.add("sn", user.value.login);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.identification.name.givenNames)) {
			ldapEntry.add("givenName", user.value.contactInfos.identification.name.givenNames);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.explanatory.note)) {
			ldapEntry.add("description", user.value.contactInfos.explanatory.note);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.organizational.org.company)) {
			ldapEntry.add("o", user.value.contactInfos.organizational.org.company);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.organizational.org.division)) {
			ldapEntry.add("ou", user.value.contactInfos.organizational.org.division);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.organizational.org.department)) {
			ldapEntry.add("departmentNumber", user.value.contactInfos.organizational.org.department);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.organizational.title)) {
			ldapEntry.add("title", user.value.contactInfos.organizational.title);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.organizational.role)) {
			ldapEntry.add("employeeType", user.value.contactInfos.organizational.role);
		}

		if (userPhoto != null && userPhoto.length != 0) {
			ldapEntry.add("jpegPhoto", userPhoto);
		}
	}

	@Override
	public String getDn() {
		String parentDn = new DomainDirectoryUsers(domain).getDn();
		return getRDn() + "," + parentDn;
	}

	@Override
	public String getRDn() {
		return RDN_ATTRIBUTE + "=" + getRDnValue();
	}

	public String getRDnValue() {
		return user.value.login;
	}

	@Override
	public Entry getLdapEntry() throws ServerFault {
		Entry ldapEntry;

		try {
			ldapEntry = new DefaultEntry(getDn(), "objectclass: inetOrgPerson", "objectclass: ShadowAccount",
					"objectclass: bmUser");

			ldapEntry.add("bmHidden", Boolean.toString(user.value.hidden));

			initIdentity(ldapEntry);
			initEmail(ldapEntry);
			initPhone(ldapEntry);
			initAddress(ldapEntry);
			initPassword(ldapEntry);

			for (IEntityEnhancer entityEnhancer : Activator.getEntityEnhancerHooks()) {
				Entry enhancedEntry = entityEnhancer.enhanceUser(domain, user, ldapEntry);
				if (enhancedEntry != null) {
					ldapEntry = enhancedEntry;
				}
			}

			if (ldapEntry.get(BMUID) != null) {
				ldapEntry.removeAttributes(BMUID);
			}
			ldapEntry.add(BMUID, user.uid);
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage user: " + getDn(), e);
		}

		return ldapEntry;
	}

	private String getFormatedRegisteredAddress(Address address) {
		ArrayList<String> registeredAddress = new ArrayList<>();
		if (!Strings.isNullOrEmpty(address.streetAddress)) {
			registeredAddress.add(address.streetAddress);
		}
		if (!Strings.isNullOrEmpty(address.postOfficeBox)) {
			registeredAddress.add(address.postOfficeBox);
		}

		StringBuilder townCountryPostal = townCountryPostal(address);
		if (!townCountryPostal.isEmpty()) {
			registeredAddress.add(townCountryPostal.toString());
		}

		if (!Strings.isNullOrEmpty(address.countryName)) {
			registeredAddress.add(address.countryName);
		}

		if (registeredAddress.isEmpty()) {
			return null;
		}

		return String.join("$", registeredAddress);
	}

	private StringBuilder townCountryPostal(Address address) {
		StringBuilder townCountry = new StringBuilder();
		if (!Strings.isNullOrEmpty(address.locality)) {
			townCountry.append(address.locality);
		}

		StringBuilder countryPostal = new StringBuilder();
		if (!Strings.isNullOrEmpty(address.countryName)) {
			countryPostal.append(address.countryName);
		}

		if (!Strings.isNullOrEmpty(address.postalCode)) {
			if (!countryPostal.isEmpty()) {
				countryPostal.append(" ");
			}

			countryPostal.append(address.postalCode);
		}

		if (!countryPostal.isEmpty()) {
			if (!townCountry.isEmpty()) {
				townCountry.append(", ");
			}

			townCountry.append(countryPostal);
		}

		return townCountry;
	}

	@Override
	public ModifyRequest getModifyRequest(Entry currentEntry) throws ServerFault {
		ModifyRequest modifyRequest = new ModifyRequestImpl();
		modifyRequest.setName(currentEntry.getDn());

		Entry entry = getLdapEntry();

		for (String attr : Stream.concat(ldapAttrsStringsValues.stream(), getEnhancerAttributeList().stream())
				.map(String::toLowerCase).collect(Collectors.toSet())) {
			modifyRequest = updateLdapAttribute(modifyRequest, currentEntry, entry, attr);
		}

		return modifyRequest;
	}

	private Collection<String> getEnhancerAttributeList() {
		return Activator.getEntityEnhancerHooks().stream().map(IEntityEnhancer::userEnhancerAttributes)
				.filter(Objects::nonNull).flatMap(List::stream).collect(Collectors.toSet());
	}
}

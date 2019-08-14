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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

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
	private static final String USER_ARCHIVED_EMPLOYEETYPE = "archived";

	private final ItemValue<Domain> domain;
	private final ItemValue<User> user;
	private final byte[] userPhoto;

	public static final List<String> ldapAttrsStringsValues = ImmutableList.of( //
			"objectclass",
			// Identity
			"bmUid", "bmHidden", "cn", "displayName", "sn", "employeeType", "givenName", "description", "ou",
			"departmentNumber", "title", "jpegPhoto",
			// Email
			"mail",
			// Phones
			"telephoneNumber", "facsimileTelephoneNumber", "homePhone", "mobile",
			// Address
			"l", "postalCode", "postOfficeBox", "postalAddress", "street", "st", "registeredAddress",
			// Password
			"userPassword");

	public DomainDirectoryUser(ItemValue<Domain> domain, ItemValue<User> user, byte[] userPhoto) {
		this.domain = domain;
		this.user = user;
		this.userPhoto = userPhoto;
	}

	public DomainDirectoryUser(ItemValue<Domain> domain, ItemValue<User> user) {
		this.domain = domain;
		this.user = user;
		this.userPhoto = null;
	}

	private void initPassword(Entry ldapEntry) throws LdapException {
		ldapEntry.add("userPassword", "{SASL}" + user.value.login + "@" + domain.value.name);
	}

	private void initAddress(Entry ldapEntry) throws LdapException {
		if (user.value.contactInfos.deliveryAddressing.size() == 0) {
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
		if (!Strings.isNullOrEmpty(registeredAddress)) {
			ldapEntry.add("registeredAddress", registeredAddress);
		}
	}

	private void initPhone(Entry ldapEntry) throws LdapException {
		List<Tel> tels = user.value.contactInfos.communications.tels;

		for (Tel tel : tels) {
			String attr = getPhoneLdapAttrName(tel.parameters);
			if (attr == null) {
				continue;
			}

			ldapEntry.add(attr, tel.value);
		}
	}

	private String getPhoneLdapAttrName(List<Parameter> parameters) {
		Set<String> params = parameters.stream().filter(p -> p.label.equals("TYPE")).map(p -> p.value.toLowerCase())
				.collect(Collectors.toSet());

		if (params.contains("work") && params.contains("voice")) {
			return "telephoneNumber";
		} else if (params.contains("work") && params.contains("fax")) {
			return "facsimileTelephoneNumber";
		} else if (params.contains("home") && params.contains("voice")) {
			return "homePhone";
		} else if (params.contains("cell") && params.contains("voice")) {
			return "mobile";
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

		if (user.value.archived) {
			ldapEntry.add("employeeType", USER_ARCHIVED_EMPLOYEETYPE);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.identification.name.givenNames)) {
			ldapEntry.add("givenName", user.value.contactInfos.identification.name.givenNames);
		}

		if (!Strings.isNullOrEmpty(user.value.contactInfos.explanatory.note)) {
			ldapEntry.add("description", user.value.contactInfos.explanatory.note);
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
			ldapEntry = new DefaultEntry(getDn(), "objectclass: inetOrgPerson", "objectclass: bmUser");

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

			if (ldapEntry.get("bmUid") != null) {
				ldapEntry.removeAttributes("bmUid");
			}
			ldapEntry.add("bmUid", user.uid);
		} catch (LdapException e) {
			throw new ServerFault("Fail to manage user: " + getDn(), e);
		}

		return ldapEntry;
	}

	private String getFormatedRegisteredAddress(Address address) {
		ArrayList<String> registeredAddress = new ArrayList<String>();
		if (!Strings.isNullOrEmpty(address.streetAddress)) {
			registeredAddress.add(address.streetAddress);
		}
		if (!Strings.isNullOrEmpty(address.postOfficeBox)) {
			registeredAddress.add(address.postOfficeBox);
		}

		ArrayList<String> townCountry = new ArrayList<String>();
		if (!Strings.isNullOrEmpty(address.locality)) {
			townCountry.add(address.locality);
		}

		ArrayList<String> country = new ArrayList<String>();
		if (!Strings.isNullOrEmpty(address.countryName)) {
			country.add(address.countryName);
		}

		if (!Strings.isNullOrEmpty(address.postalCode)) {
			country.add(address.postalCode);
		}

		StringBuilder formated = new StringBuilder();
		Iterator<String> iter = country.iterator();
		while (iter.hasNext()) {
			formated.append(iter.next());
			if (iter.hasNext()) {
				formated.append(" ");
			}
		}

		if (formated.length() != 0) {
			townCountry.add(formated.toString());
		}

		formated = new StringBuilder();
		iter = townCountry.iterator();
		while (iter.hasNext()) {
			formated.append(iter.next());
			if (iter.hasNext()) {
				formated.append(", ");
			}
		}

		if (formated.length() != 0) {
			registeredAddress.add(formated.toString());
		}

		if (!Strings.isNullOrEmpty(address.countryName)) {
			registeredAddress.add(address.countryName);
		}

		formated = new StringBuilder();
		iter = registeredAddress.iterator();
		while (iter.hasNext()) {
			formated.append(iter.next());
			if (iter.hasNext()) {
				formated.append("$");
			}
		}

		return formated.toString();
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

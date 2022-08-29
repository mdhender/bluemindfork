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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public abstract class EntityManager {
	public final ItemValue<Domain> domain;

	public EntityManager(ItemValue<Domain> domain) {
		this.domain = domain;
	}

	protected Set<String> getDomainAliases() {
		Set<String> domainAliases = new HashSet<>(domain.value.aliases);
		domainAliases.add(domain.value.name);

		return domainAliases;
	}

	protected List<String> getAttributesValues(Entry entry, String[] attrs) {
		List<String> attrsValues = new ArrayList<>();

		for (String adMailAttr : attrs) {
			Attribute mailAttr = entry.get(adMailAttr);
			if (mailAttr == null) {
				continue;
			}

			Iterator<Value> adIterator = mailAttr.iterator();
			while (adIterator.hasNext()) {
				String userEmail = adIterator.next().getString().trim().toLowerCase();
				if (userEmail.isEmpty()) {
					continue;
				}

				attrsValues.add(userEmail);
			}
		}

		return attrsValues;
	}

	/**
	 * Return first value of attribute
	 * 
	 * @param entry
	 * @param attributeName
	 * @return
	 */
	protected String getAttributeValue(Entry entry, String attributeName) {
		Attribute attribute = entry.get(attributeName);
		if (attribute == null) {
			return null;
		}

		String attributeValue = null;
		Iterator<Value> iterator = attribute.iterator();
		if (iterator.hasNext()) {
			attributeValue = iterator.next().getString().trim();
			if (attributeValue.isEmpty()) {
				attributeValue = null;
			}
		}

		return attributeValue;
	}

	protected boolean isLocalEmail(String mail) {
		String emailDomain = "";
		if (mail.contains("@")) {
			emailDomain = mail.split("@")[1];
		}

		return emailDomain.isEmpty() || getDomainAliases().contains(emailDomain);
	}

	protected String getEmailLeftPart(String email) {
		return email.split("@")[0];
	}

	protected Set<String> getEmailRightParts(String email) {
		if (email.contains("@")) {
			return new HashSet<>(Arrays.asList(email.substring(email.indexOf('@') + 1)));
		}

		return getDomainAliases();
	}

	protected Set<String> mergeEmailRightParts(Set<String> a, Set<String> b) {
		a.addAll(b);
		return a;
	}

	protected String getDefaultLocalEmail(List<String> userEmails) {
		String defaultLocalEmail = userEmails.stream().filter(userEmail -> isLocalEmail(userEmail)).findFirst()
				.orElse(userEmails.get(0));

		if (!defaultLocalEmail.contains("@")) {
			defaultLocalEmail += "@" + domain.value.name;
		}

		return defaultLocalEmail;
	}
}

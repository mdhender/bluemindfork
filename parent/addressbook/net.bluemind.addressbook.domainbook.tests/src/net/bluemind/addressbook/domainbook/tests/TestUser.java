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
package net.bluemind.addressbook.domainbook.tests;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.RandomStringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.Email;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;

public class TestUser {

	private User value;
	private String domainUid;

	private TestUser(String domainUid) {
		this.domainUid = domainUid;
		value = new User();
		value.contactInfos = new VCard();
		value.login = randomLogin();
		defaultEmail();
		value.password = "password";
		value.routing = Routing.internal;
	}

	private void defaultEmail() {
		Email em = new Email();
		em.address = value.login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		value.emails = Arrays.asList(em);

	}

	private String randomLogin() {
		return RandomStringUtils.randomAlphanumeric(10);
	}

	public static TestUser of(String domainUid) {
		return new TestUser(domainUid);
	}

	public TestUser login(String login) {
		value.login = login;
		defaultEmail();
		return this;
	}

	public User build() {
		return value;
	}

	public TestUser alias(String email) {
		Email em = new Email();
		em.address = email;
		em.isDefault = false;
		em.allAliases = false;
		if (value.emails == null) {
			value.emails = Arrays.asList(em);
		} else {
			value.emails = new ArrayList<>(value.emails.size() + 1);
			value.emails.add(em) ;
		}
		return this;
	}
	
	public TestUser names(String firstname, String lastname) {
		value.contactInfos.identification.name = Name.create(lastname, firstname, null, null, null, null);
		value.contactInfos.identification.formatedName.value = firstname + " " + lastname;
		return this;
	}

	public TestUser hidden() {
		value.hidden = true;
		return this;
	}

	public TestUser archived() {
		value.archived = true;
		return this;
	}
}

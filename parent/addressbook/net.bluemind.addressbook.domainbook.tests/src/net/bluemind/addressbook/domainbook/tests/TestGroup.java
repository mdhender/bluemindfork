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

import java.util.Arrays;

import org.apache.commons.lang.RandomStringUtils;

import net.bluemind.core.api.Email;
import net.bluemind.group.api.Group;

public class TestGroup {

	private Group value;
	private String domainUid;

	private TestGroup(String domainUid) {
		this.domainUid = domainUid;
		value = new Group();
		value.name = randomName();
		defaultEmail();

	}

	private void defaultEmail() {
		Email em = new Email();
		em.address = value.name + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		value.emails = Arrays.asList(em);

	}

	private String randomName() {
		return RandomStringUtils.randomAlphanumeric(10);
	}

	public static TestGroup of(String domainUid) {
		return new TestGroup(domainUid);
	}

	public Group build() {
		return value;
	}

	public TestGroup hidden() {
		value.hidden = true;
		return this;
	}

	public TestGroup archived() {
		value.archived = true;
		return this;
	}

	public TestGroup name(String name) {
		value.name = name;
		return this;
	}
}

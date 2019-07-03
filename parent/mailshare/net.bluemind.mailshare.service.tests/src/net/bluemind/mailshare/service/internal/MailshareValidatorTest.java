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
package net.bluemind.mailshare.service.internal;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareValidatorTest {

	private MailshareValidator validator = new MailshareValidator();

	@Test
	public void testNominal() {
		Mailshare ms = defaultMailshare();
		validateNotFail(ms);

	}

	@Test
	public void testNull() {
		validateFail(null);
	}

	@Test
	public void tesMailshareName() {
		Mailshare ms = defaultMailshare();
		ms.name = "";
		validateFail(ms);

		ms.name = "Parte";
		validateFail(ms);

		ms.name = "papi et manie";
		validateFail(ms);

		ms.name = "partenaire-de-qalité";
		validateFail(ms);

		ms.name = "partenaire-de-qalite";
		validateNotFail(ms);
	}

	@Test
	public void tesMailshareEmptyEmailList() {
		Mailshare ms = defaultMailshare();
		ms.emails = Collections.emptyList();
		validateNotFail(ms);

		ms.emails = null;
		validateNotFail(ms);
	}

	@Test
	public void testRouting_None() {
		Mailshare ms = defaultMailshare();
		ms.routing = Routing.none;
		validateNotFail(ms);
	}

	@Test
	public void testRouting_Internal() {
		Mailshare ms = defaultMailshare();
		ms.routing = Routing.internal;
		validateNotFail(ms);
	}

	@Test
	public void testRouting_External() {
		Mailshare ms = defaultMailshare();
		ms.routing = Routing.external;
		validateNotFail(ms);
	}

	@Test
	public void testRouting_Null() {
		Mailshare ms = defaultMailshare();
		ms.routing = null;
		validateFail(ms);
	}

	private void validateNotFail(Mailshare ms) {

		try {
			validator.validate(ms);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail();
		}
	}

	private void validateFail(Mailshare ms) {
		try {
			validator.validate(ms);
			fail();
		} catch (ServerFault e) {

		}
	}

	private Mailshare defaultMailshare() {
		Mailshare ms = new Mailshare();
		ms.name = "test";
		ms.dataLocation = "serverUid";
		ms.emails = Arrays.asList(Email.create("test@bm.lan", true));
		ms.routing = Routing.internal;
		return ms;
	}

}

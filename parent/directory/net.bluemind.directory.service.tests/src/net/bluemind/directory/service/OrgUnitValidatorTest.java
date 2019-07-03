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
package net.bluemind.directory.service;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.validator.IValidator;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.service.internal.OrgUnitValidatorFactory;

public class OrgUnitValidatorTest {

	private IValidator<OrgUnit> validator;

	@Before
	public void before() {
		validator = new OrgUnitValidatorFactory().create(null);
	}

	@Test
	public void testCreate() {
		OrgUnit ou = new OrgUnit();
		ou.name = "oOooo";
		validator.create(ou);

		ou.name = "oOooo oo";
		validator.create(ou);

		ou.name = "oOooo-oo";
		validator.create(ou);

		ou.name = "oOooo_oo";
		validator.create(ou);

	}

	@Test
	public void testUpdate() {
		OrgUnit ou = new OrgUnit();
		ou.name = "oOooo";
		validator.update(ou, ou);

		ou.name = "oOooo oo";
		validator.update(ou, ou);

		ou.name = "oOooo-oo";
		validator.update(ou, ou);

		ou.name = "oOooo_oo";
		validator.update(ou, ou);

	}
}

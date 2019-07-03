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
package net.bluemind.resource.service.internal;

import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property.Type;

public class ResourceTypeValidatorTests {

	private ResourceTypesValidator validator = new ResourceTypesValidator();

	@Test
	public void testNominal() {
		validateNotFail(ResourceTypeDescriptor.create("test 1 2 3",
				ResourceTypeDescriptor.Property.create("test", Type.Boolean, "test")));
	}

	@Test
	public void testNull() {
		validateFail(null);
	}

	@Test
	public void testNotValid() {
		validateFail(ResourceTypeDescriptor.create(null));
		validateFail(ResourceTypeDescriptor.create("test",
				ResourceTypeDescriptor.Property.create("test", Type.Boolean, "")));
		validateFail(ResourceTypeDescriptor.create("test",
				ResourceTypeDescriptor.Property.create("", Type.Boolean, "test")));
		validateFail(
				ResourceTypeDescriptor.create("test", ResourceTypeDescriptor.Property.create("test", null, "test")));
	}

	private void validateNotFail(ResourceTypeDescriptor rtd) {

		try {
			validator.validate(rtd);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail();
		}
	}

	private void validateFail(ResourceTypeDescriptor rtd) {
		try {
			validator.validate(rtd);
			fail();
		} catch (ServerFault e) {

		}
	}

}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property.Type;

public class ResourceValidatorTest {

	private ResourceValidator validator = new ResourceValidator();

	@Test
	public void testValidateNominal() {
		ResourceDescriptor rd = defaultDescriptor();
		validateNotFail(rd);

		rd = defaultDescriptor();
		rd.properties = Arrays.<ResourceDescriptor.PropertyValue> asList();
		validateNotFail(rd);
	}

	@Test
	public void testNotValid() {
		validateFail(null);
		ResourceDescriptor rd = defaultDescriptor();
		rd.label = null;
		validateFail(rd);

		rd = defaultDescriptor();
		rd.dataLocation = "";
		validateFail(rd);

		rd = defaultDescriptor();
		rd.typeIdentifier = "";
		validateFail(rd);

		rd = defaultDescriptor();
		rd.properties = new ArrayList<>();
		rd.properties.add(ResourceDescriptor.PropertyValue.create("", "test1"));
		validateFail(rd);

		rd = defaultDescriptor();
		rd.emails = Collections.emptyList();
		validateFail(rd);
	}

	@Test
	public void testValidatePropertiesValue() {

		ResourceTypeDescriptor typeDescriptor = ResourceTypeDescriptor.create("testType",
				ResourceTypeDescriptor.Property.create("test1", Type.String, ""),
				ResourceTypeDescriptor.Property.create("test2", Type.Boolean, ""),
				ResourceTypeDescriptor.Property.create("test3", Type.Number, ""),
				ResourceTypeDescriptor.Property.create("test4", Type.String, ""));
		ResourceDescriptor rd = defaultDescriptor();
		validatePropertiesValueNotFail(rd, typeDescriptor);
	}

	@Test
	public void testValidatePropertiesValueNotValue() {

		ResourceTypeDescriptor typeDescriptor = ResourceTypeDescriptor.create("testType", //
				ResourceTypeDescriptor.Property.create("test1", Type.String, ""), //
				ResourceTypeDescriptor.Property.create("test2", Type.Boolean, ""), //
				ResourceTypeDescriptor.Property.create("test3", Type.Number, ""), //
				ResourceTypeDescriptor.Property.create("test4", Type.String, ""));
		ResourceDescriptor rd = defaultDescriptor();

		rd.properties.get(1).value = "tada";
		validatePropertiesValueFail(rd, typeDescriptor);

		rd = defaultDescriptor();

		rd.properties.get(2).value = "tada";
		validatePropertiesValueFail(rd, typeDescriptor);
	}

	private void validatePropertiesValueNotFail(ResourceDescriptor rd, ResourceTypeDescriptor typeDescriptor) {
		try {
			validator.validatePropertiesValue(rd, typeDescriptor);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail();
		}
	}

	private void validatePropertiesValueFail(ResourceDescriptor rd, ResourceTypeDescriptor typeDescriptor) {
		try {
			validator.validatePropertiesValue(rd, typeDescriptor);
			fail();
		} catch (ServerFault e) {

		}
	}

	private void validateNotFail(ResourceDescriptor rtd) {

		try {
			validator.validate(rtd);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail();
		}
	}

	private void validateFail(ResourceDescriptor rtd) {
		try {
			validator.validate(rtd);
			fail();
		} catch (ServerFault e) {

		}
	}

	private ResourceDescriptor defaultDescriptor() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "test 1";
		rd.description = "hi !";
		rd.dataLocation = "serverUid";
		rd.typeIdentifier = "testType";
		rd.emails = Arrays.asList(Email.create("test@bm.lan", true));
		rd.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "true"),
				ResourceDescriptor.PropertyValue.create("test3", "1"),
				ResourceDescriptor.PropertyValue.create("test4", null));
		return rd;
	}
}

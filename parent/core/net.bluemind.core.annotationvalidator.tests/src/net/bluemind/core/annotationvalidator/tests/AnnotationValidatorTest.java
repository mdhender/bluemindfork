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
package net.bluemind.core.annotationvalidator.tests;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.annotationvalidator.AnnotationValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.validator.IValidatorFactory;

public class AnnotationValidatorTest {

	public static class Value {
		@NotNull
		public String aNotNullString;
	}

	private IValidatorFactory<Value> validator;

	@Before
	public void before() {
		validator = new AnnotationValidator.GenericValidatorFactory<>(Value.class);
	}

	@Test
	public void testNotNullString() {
		try {
			validator.create(null).create(new Value());
			fail("should throw");
		} catch (ServerFault e) {
			System.err.println(e.getMessage());
		}
	}
}

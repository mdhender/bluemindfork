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
package net.bluemind.todolist.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Test;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.service.internal.VTodoValidator;

public class VTodoValidatorTests {

	private VTodoValidator validator = new VTodoValidator();

	@Test
	public void validate() throws ServerFault {
		VTodo vtodo = null;
		ErrorCode err = null;

		// vtodo null
		try {
			validator.validate(vtodo);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.INVALID_PARAMETER == err);

		// dtstart null
		vtodo = new VTodo();
		err = null;
		try {
			validator.validate(vtodo);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertNull(err);

		// dtstart != null
		vtodo.dtstart = BmDateTimeWrapper.create(new DateTime(), Precision.DateTime);
		err = null;
		try {
			validator.validate(vtodo);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertNull(err);

		// rrule
		VTodo.RRule rrule = new VTodo.RRule();
		vtodo.rrule = rrule;
		try {
			validator.validate(vtodo);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertTrue(ErrorCode.INVALID_PARAMETER == err);

		rrule.frequency = VTodo.RRule.Frequency.DAILY;
		vtodo.rrule = rrule;
		err = null;
		try {
			validator.validate(vtodo);
		} catch (ServerFault e) {
			err = e.getCode();
		}
		assertNull(err);

	}
}

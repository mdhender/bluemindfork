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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.service.internal.VTodoSanitizer;

public class VTodoSanatizerTests {

	private VTodoSanitizer sanitizer = new VTodoSanitizer();

	@Test
	public void testSanitize() {
		VTodo vtodo = new VTodo();
		vtodo.priority = 10;

		sanitizer.sanitize(vtodo);
		assertNotNull(vtodo.summary);
		assertTrue(vtodo.summary.length() > 0);

		assertNotNull(vtodo.uid);
		assertTrue(vtodo.priority == 9);
		assertTrue(vtodo.percent == 0);
		vtodo.priority = -1;
		vtodo.percent = 101;

		sanitizer.sanitize(vtodo);

		assertTrue(vtodo.priority == 0);
		assertTrue(vtodo.percent == 100);
	}

}

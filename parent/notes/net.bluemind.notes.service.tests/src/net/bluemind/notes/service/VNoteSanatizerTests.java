/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.bluemind.notes.api.VNote;
import net.bluemind.notes.api.VNote.Color;
import net.bluemind.notes.service.internal.VNoteSanitizer;

public class VNoteSanatizerTests {

	private VNoteSanitizer sanitizer = new VNoteSanitizer();

	@Test
	public void testSanitize() {
		VNote vnote = new VNote();
		vnote.color = null;

		sanitizer.sanitize(vnote);
		assertNotNull(vnote.color);
		assertEquals(Color.YELLOW, vnote.color);
	}
}

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

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.notes.api.VNote;
import net.bluemind.notes.service.internal.VNoteValidator;

public class VNoteValidatorTests {

	private VNoteValidator validator = new VNoteValidator();

	@Test
	public void validate() throws ServerFault {
		VNote vnote = null;

		// vnote null
		try {
			validator.validate(vnote);
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}
}

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.imap.endpoint.imaptest.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.bluemind.imap.endpoint.parsing.Base64Splitter;

public class RoundcubeAuthenticateTests {

	@Test
	public void rcPayload() {
		String payload = "AHRvbUBmOGRlMmM0YS5pbnRlcm5hbAA1ZjM2NDMxOC0zMDVjLTQ1MmItYjhhMy0zOWNmMjJhYjA4MWE=";
		byte[] dec = Base64.getDecoder().decode(payload);
		List<String> split = Base64Splitter.splitOnNull(dec);
		System.err.println(split);
		assertEquals(3, split.size());
	}

}

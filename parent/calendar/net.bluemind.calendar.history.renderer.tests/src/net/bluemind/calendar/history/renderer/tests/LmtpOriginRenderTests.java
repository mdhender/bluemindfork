/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.calendar.history.renderer.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.calendar.history.renderer.LmtpOriginRenderer;

public class LmtpOriginRenderTests {

	@Test
	public void testRender() {
		LmtpOriginRenderer lor = new LmtpOriginRenderer();
		assertEquals("john.bang", lor.processOrigin("john.bang"));
		assertEquals("tom@devenv.blue => nico@eff86d01.internal (LMTPd)",
				lor.processOrigin("bm-lmtpd_from_tom@devenv.blue_to_nico@eff86d01.internal"));
		assertEquals("BlueMind LMTPd => nico@eff86d01.internal",
				lor.processOrigin("bm-lmtpd_to_nico@eff86d01.internal"));
		assertEquals("BlueMind LMTPd", lor.processOrigin("bm-lmtpd"));
	}

}

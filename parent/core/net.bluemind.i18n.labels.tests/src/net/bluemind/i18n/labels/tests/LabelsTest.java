/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.i18n.labels.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.i18n.labels.I18nLabels;

public class LabelsTest {

	@Test
	public void testOutOfBound() {
		String res = I18nLabels.getInstance().translate("fr", "$");
		assertEquals("$", res);
		res = I18nLabels.getInstance().translate("fr", "$$");
		assertEquals("$$", res);
		res = I18nLabels.getInstance().translate("fr", "$$$");
		assertEquals("$$$", res);
		res = I18nLabels.getInstance().translate("fr", "$$$$");
		assertEquals("$$$$", res);
		res = I18nLabels.getInstance().translate("fr", "$$mycontacts$$");
		assertEquals("Mes contacts", res);
	}

}

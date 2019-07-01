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
package net.bluemind.system.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.DomainTemplate.Kind;
import net.bluemind.system.api.DomainTemplate.Tag;
import net.bluemind.system.api.IDomainTemplate;
import net.bluemind.system.service.internal.DomainTemplateService;

public class DomainTemplateTests {

	@Test
	public void testGetDomainTemplate() throws ServerFault {
		IDomainTemplate tService = new DomainTemplateService();
		DomainTemplate template = tService.getTemplate();

		// test kind
		boolean foundTestKind = false;
		for (Kind kind : template.kinds) {
			if (kind.id.equals("test")) {
				foundTestKind = true;
				break;
			}
		}

		assertTrue(foundTestKind);

		// extend bm kind
		Kind bmKind = null;
		for (Kind kind : template.kinds) {
			if (kind.id.equals("bm")) {

				bmKind = kind;
				break;
			}
		}

		assertNotNull(bmKind);
		boolean foundTestTag = false;
		for (Tag tag : bmKind.tags) {
			if ("test".equals(tag.value)) {
				foundTestTag = true;
				break;
			}
		}

		assertTrue(foundTestTag);
	}
}

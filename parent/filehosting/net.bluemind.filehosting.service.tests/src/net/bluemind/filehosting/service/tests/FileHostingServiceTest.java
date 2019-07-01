/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.filehosting.service.internal.FileHostingService;

public class FileHostingServiceTest {

	@Test
	public void testPathValidationList() {

		FileHostingService service = new FileHostingTestService();
		try {
			service.list("../../private/");
			fail();
		} catch (Exception e) {
			assertEquals(((ServerFault) e).getCode(), ErrorCode.FORBIDDEN);
		}

		service.list("valid");
		service.list("valid/");
		service.list("valid/sub/test");
		service.list("/valid");
	}

	@Test
	public void testPathValidationGet() {

		FileHostingService service = new FileHostingTestService();
		try {
			service.get("../../private/fancypics");
			fail();
		} catch (Exception e) {
			assertEquals(((ServerFault) e).getCode(), ErrorCode.FORBIDDEN);
		}

		service.get("valid");

	}

	@Test
	public void testPathValidationShare() {

		FileHostingService service = new FileHostingTestService();
		try {
			service.share("../../private/fancypics", 1, "2018-01-01");
			fail();
		} catch (Exception e) {
			assertEquals(((ServerFault) e).getCode(), ErrorCode.FORBIDDEN);
		}

		service.share("valid", 1, "2018-01-01");

	}

	@Test
	public void testPathValidationStore() {

		FileHostingService service = new FileHostingTestService();
		try {
			service.store("../../private/fancypics", null);
			fail();
		} catch (Exception e) {
			assertEquals(((ServerFault) e).getCode(), ErrorCode.FORBIDDEN);
		}

		service.store("valid", null);

	}

	@Test
	public void testPathValidationDelete() {

		FileHostingService service = new FileHostingTestService();
		try {
			service.delete("../../private/fancypics");
			fail();
		} catch (Exception e) {
			assertEquals(((ServerFault) e).getCode(), ErrorCode.FORBIDDEN);
		}

		service.delete("valid");

	}

}

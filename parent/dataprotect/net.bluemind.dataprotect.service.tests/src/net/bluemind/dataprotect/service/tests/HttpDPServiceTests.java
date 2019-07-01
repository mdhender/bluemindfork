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

package net.bluemind.dataprotect.service.tests;

import org.junit.Before;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;

public class HttpDPServiceTests extends DPServiceTests {

	@Before
	public void before() throws Exception {
		super.before();
	}

	public void after() throws Exception {
		super.after();
	}

	@Override
	protected IServiceProvider fromContext(BmContext ctx) {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", ctx.getSecurityContext().getSessionId());
	}

}

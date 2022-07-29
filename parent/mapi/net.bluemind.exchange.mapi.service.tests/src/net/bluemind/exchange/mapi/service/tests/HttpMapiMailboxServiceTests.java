/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.exchange.mapi.service.tests;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.lib.vertx.VertxPlatform;

public class HttpMapiMailboxServiceTests extends MapiMailboxServiceTests {

	@Before
	public void before() throws Exception {
		super.before();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		Sessions.get().put(userSecurityContext.getSessionId(), userSecurityContext);
	}

	@After
	public void after() throws Exception {
		super.after();
	}

	protected IMapiMailbox mapiMboxApi() {
		ClientSideServiceProvider sp = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090",
				userSecurityContext.getSessionId());
		IMapiMailbox service = sp.instance(IMapiMailbox.class, domainUid, mailbox.uid);
		return service;
	}

	protected IContainersFlatHierarchy hierarchyApi() {
		ClientSideServiceProvider sp = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090",
				userSecurityContext.getSessionId());
		return sp.instance(IContainersFlatHierarchy.class, domainUid, userUid);
	}

}

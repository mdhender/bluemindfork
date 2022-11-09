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
package net.bluemind.addressbook.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AddressBookDescSanitizerTests {

	private SecurityContext domainAdmin;
	private String domainUid;
	private ItemValue<Domain> testDomain;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		domainUid = "test" + System.currentTimeMillis() + ".lan";

		domainAdmin = BmTestContext.contextWithSession("testUser", "test", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		testDomain = PopulateHelper.createTestDomain(domainUid, esServer);
		PopulateHelper.domainAdmin(domainUid, domainAdmin.getSubject());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void testSanitize() throws ServerFault {
		String abUid = "testSanitize" + System.currentTimeMillis();
		IAddressBooksMgmt service = service(domainAdmin);

		AddressBookDescriptor bookDescriptor = AddressBookDescriptor.create("test", domainUid,
				testDomain.value.defaultAlias, null);

		service.create(abUid, bookDescriptor, false);
		AddressBookDescriptor book = service.getComplete(abUid);
		assertNotNull(book);
		assertNotNull(book.settings);
		assertEquals(domainUid, book.domainUid);
	}

	private IAddressBooksMgmt service(SecurityContext sc) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IAddressBooksMgmt.class, domainUid);
	}
}

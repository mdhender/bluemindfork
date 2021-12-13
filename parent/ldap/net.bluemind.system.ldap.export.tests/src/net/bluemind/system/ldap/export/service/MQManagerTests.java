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
package net.bluemind.system.ldap.export.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.verticle.MQManager;

public class MQManagerTests extends LdapExportTests {
	@Test
	public void handle() throws ServerFault, LdapException {
		domain.value.label = "This is new label";
		domain.value.description = "This is new description";

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class, InstallationId.getIdentifier()).update(domain.uid, domain.value);

		Entry entry = LdapHelper.connectDirectory(ldapRoleServer).lookup(String.format("dc=%s,dc=local", domain.uid));
		assertNotNull(entry);
		assertNull(entry.get("description"));
		assertEquals(1, entry.get("o").size());
		assertEquals(domain.uid, entry.get("o").get().getString());
		assertEquals(1, entry.get("bmVersion").size());
		String bmVersion = entry.get("bmVersion").get().getString();

		new MQManager().handle(new OOPMessage(new JsonObject()).putStringProperty("operation", "domain.updated")
				.putStringProperty("domain", domain.uid));

		entry = LdapHelper.connectDirectory(ldapRoleServer).lookup(String.format("dc=%s,dc=local", domain.uid));
		assertNotNull(entry);

		assertEquals(3, entry.get("objectClass").size());
		List<String> expectedOC = Arrays.asList("organization", "dcobject", "bmdomain");
		entry.get("objectClass").forEach(v -> assertTrue(expectedOC.contains(v.getString().toLowerCase())));

		assertEquals(1, entry.get("dc").size());
		assertEquals(domain.uid, entry.get("dc").get().getString());

		assertEquals(1, entry.get("o").size());
		assertEquals("This is new label", entry.get("o").get().getString());

		assertEquals(1, entry.get("description").size());
		assertEquals("This is new description", entry.get("description").get().getString());

		assertEquals(1, entry.get("bmVersion").size());
		assertEquals(bmVersion, entry.get("bmVersion").get().getString());
	}
}

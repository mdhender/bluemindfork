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
package net.bluemind.keycloak.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class KerberosConfigurationTests extends AbstractServiceTests {

	@Test
	public void testExternalUrlConfiguration() throws Exception {
		getKeycloakAdminService().createRealm("global.virt");
		getKeycloakClientAdminService("global.virt").create("global.virt-cli");

		String domainUid = "bm.lan";
		PopulateHelper.createDomain(domainUid);

		ItemValue<Domain> domain = getDomainService().get(domainUid);
		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.KERBEROS.name());
		domain.value.properties.put(AuthDomainProperties.KRB_AD_DOMAIN.name(), domainUid.toUpperCase());
		domain.value.properties.put(AuthDomainProperties.KRB_AD_IP.name(), "192.168.0.111");
		domain.value.properties.put(AuthDomainProperties.KRB_KEYTAB.name(),
				"VGhpcyBpcyBzdXBwb3NlZCB0byBiZSBhIGtleXRhYiBmaWxlLg==");
		getDomainService().update(domainUid, domain.value);

		Thread.sleep(3000); // NOOOOOOOO

		KerberosComponent kerberosComponent = getKeycloakKerberosService("global.virt")
				.getKerberosProvider("global.virt-kerberos");
		assertNotNull(kerberosComponent);
		assertEquals(domainUid.toUpperCase(), kerberosComponent.getKerberosRealm());

		domainUid = "another.lan";
		PopulateHelper.createDomain(domainUid);
		domain = getDomainService().get(domainUid);
		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.KERBEROS.name());
		domain.value.properties.put(AuthDomainProperties.KRB_AD_DOMAIN.name(), domainUid.toUpperCase());
		domain.value.properties.put(AuthDomainProperties.KRB_AD_IP.name(), "192.168.0.222");
		domain.value.properties.put(AuthDomainProperties.KRB_KEYTAB.name(), "U29tZSBmYWtlIGtleXRhYiBmaWxlLCB5byE=");

		try {
			getDomainService().update(domainUid, domain.value);
			fail("Adding a second kerberos domain without an external URL shouldn't have been allowed");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_AUTH_PARAMETER, e.getCode());
		}

		Map<String, String> domainSettings = getDomainSettingsService(domainUid).get();
		domainSettings.put(DomainSettingsKeys.external_url.name(), "mail.another.lan");
		getDomainSettingsService(domainUid).set(domainSettings);

		getDomainService().update(domainUid, domain.value);

		Thread.sleep(3000); // NOOOOOOOO

		kerberosComponent = getKeycloakKerberosService(domainUid).getKerberosProvider(domainUid + "-kerberos");
		assertNotNull(kerberosComponent);
		assertEquals(domainUid.toUpperCase(), kerberosComponent.getKerberosRealm());
	}

}

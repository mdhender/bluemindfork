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
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CasConfigurationTests extends AbstractServiceTests {

	@Test
	public void testTwoDomainsOneCas() throws Exception {

		// Two domains without external urls
		String domainUid = "bm.lan";
		PopulateHelper.createDomain(domainUid);
		PopulateHelper.createDomain("osef.lan");

		// update one domain to CAS authentication is forbidden
		ItemValue<Domain> domain = getDomainService().get(domainUid);
		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.CAS.name());
		domain.value.properties.put(AuthDomainProperties.CAS_URL.name(), "https://cas.url/");

		try {
			getDomainService().update(domainUid, domain.value);
			fail("Update domain to cas auth is forbidden. Missing external url");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_AUTH_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testOneCasDomainWithoutExternalUrlCreateNewDomain() throws Exception {

		// One CAS domain without external url
		String domainUid = "cas.lan";
		PopulateHelper.createDomain(domainUid);
		ItemValue<Domain> domain = getDomainService().get(domainUid);
		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.CAS.name());
		domain.value.properties.put(AuthDomainProperties.CAS_URL.name(), "https://cas.url/");
		getDomainService().update(domainUid, domain.value);

		// Create another domain without external url is forbidden
		try {
			PopulateHelper.createDomain("another.lan");
			fail("Create another domain without external url is forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_AUTH_PARAMETER, e.getCode());
		}
	}

	@Test
	public void testOneCasDomainWithExternalUrlOneDomainWithoutExternalUrlRemoveExternalUrlFromCasDomain()
			throws Exception {

		// One domain with external url
		String domainUid = "bm.lan";
		PopulateHelper.createDomain(domainUid);
		Map<String, String> domainSettings = getDomainSettingsService(domainUid).get();
		domainSettings.put(DomainSettingsKeys.external_url.name(), "mail.bm.lan");
		getDomainSettingsService(domainUid).set(domainSettings);

		// One CAS Domain without external url
		String casDomainUid = "cas.lan";
		PopulateHelper.createDomain(casDomainUid);
		ItemValue<Domain> domain = getDomainService().get(casDomainUid);
		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.CAS.name());
		domain.value.properties.put(AuthDomainProperties.CAS_URL.name(), "https://cas.url/");
		getDomainService().update(casDomainUid, domain.value);

		// remove external url from domain should fail
		domainSettings = getDomainSettingsService(domainUid).get();
		domainSettings.remove(DomainSettingsKeys.external_url.name());
		try {
			getDomainSettingsService(domainUid).set(domainSettings);
			fail("Nein");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_AUTH_PARAMETER, e.getCode());
		}

	}

}

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

public class ExternalAuthConfigurationTests extends AbstractServiceTests {

	@Test
	public void testExternalUrlConfiguration() throws Exception {
		String domainUid = "bm.lan";
		PopulateHelper.createDomain(domainUid);

		ItemValue<Domain> domain = getDomainService().get(domainUid);

		domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.OPENID.name());
		domain.value.properties.put(AuthDomainProperties.OPENID_CLIENT_ID.name(), "clientId");
		domain.value.properties.put(AuthDomainProperties.OPENID_CLIENT_SECRET.name(), "secret");
		domain.value.properties.put(AuthDomainProperties.OPENID_HOST.name(), "http://openid.host/");

		try {
			getDomainService().update(domainUid, domain.value);
			fail("Domain is missing an external URL");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_AUTH_PARAMETER, e.getCode());
		}

		Map<String, String> domainSettings = getDomainSettingsService(domainUid).get();
		domainSettings.put(DomainSettingsKeys.external_url.name(), "mail.bm.lan");
		getDomainSettingsService(domainUid).set(domainSettings);

		try {
			getDomainService().update(domainUid, domain.value);
		} catch (ServerFault sf) {
			fail(sf.getMessage());
		}

	}
}

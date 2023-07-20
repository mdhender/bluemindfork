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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

import com.google.common.base.Strings;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.api.Realm;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class KeycloakDomainHookTests extends AbstractServiceTests {

	@Test
	public void testOnDomainCreate() throws Exception {
		String domainUid = "bm.lan";
		PopulateHelper.addDomain(domainUid);

		Realm realm = getKeycloakAdminService().getRealm(domainUid);
		assertNotNull(realm);
		assertEquals(domainUid, realm.id);

		String cli = IKeycloakUids.clientId(domainUid);
		OidcClient client = getKeycloakClientAdminService(domainUid).getOidcClient(cli);
		assertNotNull(client);
		assertEquals(1, client.redirectUris.size());
		assertEquals("https://configure_external_url_in_bluemind", client.redirectUris.get(0)); // Fake redirect uri

		String secret = getKeycloakClientAdminService(domainUid).getSecret(cli);
		assertEquals(client.secret, secret);

		String providerName = IKeycloakUids.bmProviderId(domainUid);
		BluemindProviderComponent provider = getKeycloakBluemindProviderService(domainUid)
				.getBluemindProvider(providerName);
		assertNotNull(provider);
		assertTrue(provider.isEnabled());

		ItemValue<Domain> domain = getDomainService().get(domainUid);

		assertDomainProperties(domainUid, cli, secret, domain.value.properties);
	}

	@Test
	public void testChangeDomainExternalUrl() throws Exception {
		String domainUid = "bm.lan";
		PopulateHelper.addDomain(domainUid);

		String domainExternalUrl = "mail.bm.lan";

		Map<String, String> settings = getDomainSettingsService(domainUid).get();
		settings.put(DomainSettingsKeys.external_url.name(), domainExternalUrl);
		getDomainSettingsService(domainUid).set(settings);

		Thread.sleep(3000); // NOOOOOOOO

		String cli = IKeycloakUids.clientId(domainUid);
		OidcClient client = getKeycloakClientAdminService(domainUid).getOidcClient(cli);
		assertEquals(1, client.redirectUris.size());
		assertEquals("https://" + domainExternalUrl + "/auth/openid", client.redirectUris.get(0));

		domainExternalUrl = "updated.bm.lan";
		settings = getDomainSettingsService(domainUid).get();
		settings.put(DomainSettingsKeys.external_url.name(), domainExternalUrl);
		getDomainSettingsService(domainUid).set(settings);

		Thread.sleep(3000); // NOOOOOOOO

		cli = IKeycloakUids.clientId(domainUid);
		client = getKeycloakClientAdminService(domainUid).getOidcClient(cli);
		assertEquals(1, client.redirectUris.size());
		assertEquals("https://" + domainExternalUrl + "/auth/openid", client.redirectUris.get(0));
	}

	@Test
	public void testOnDomainDelete() throws Exception {
		String domainUid = "bm.lan";
		PopulateHelper.addDomain(domainUid);

		IDomains domainService = getDomainService();
		try {
			TaskRef taskRef = domainService.deleteDomainItems(domainUid);
			TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);
			assertTrue(status.state.succeed);
			domainService.delete(domainUid);
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Must not thrown an exception");
		}
		Realm realm = getKeycloakAdminService().getRealm(domainUid);
		assertNull(realm);
	}

	private void assertDomainProperties(String domainUid, String cli, String secret, Map<String, String> properties) {
		assertEquals(AuthTypes.INTERNAL.name(), properties.get(AuthDomainProperties.AUTH_TYPE.name()));
		assertEquals(domainUid, properties.get(AuthDomainProperties.OPENID_REALM.name()));
		assertEquals(cli, properties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()));
		assertEquals(secret, properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name()));

		String keycloakHost = "https://" + new BmConfIni().get("keycloak") + "/keycloak/realms/" + domainUid;
		assertEquals(keycloakHost + "/protocol/openid-connect/auth",
				properties.get(AuthDomainProperties.OPENID_AUTHORISATION_ENDPOINT.name()));
		assertEquals(keycloakHost + "/protocol/openid-connect/token",
				properties.get(AuthDomainProperties.OPENID_TOKEN_ENDPOINT.name()));
		assertEquals(keycloakHost + "/protocol/openid-connect/logout",
				properties.get(AuthDomainProperties.OPENID_END_SESSION_ENDPOINT.name()));
		assertEquals(keycloakHost + "/protocol/openid-connect/certs",
				properties.get(AuthDomainProperties.OPENID_JWKS_URI.name()));
		assertEquals(keycloakHost, properties.get(AuthDomainProperties.OPENID_ISSUER.name()));

		assertTrue(Strings.isNullOrEmpty(properties.get(AuthDomainProperties.CAS_URL.name())));
		assertTrue(Strings.isNullOrEmpty(properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name())));
		assertTrue(Strings.isNullOrEmpty(properties.get(AuthDomainProperties.KRB_AD_IP.name())));
		assertTrue(Strings.isNullOrEmpty(properties.get(AuthDomainProperties.KRB_KEYTAB.name())));
	}

}

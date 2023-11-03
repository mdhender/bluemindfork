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

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.bluemind.keycloak.api.AuthenticationFlow;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.Component.CachePolicy;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.KerberosComponent;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.api.Realm;

public class KeycloakServiceTests extends AbstractServiceTests {

	@Test
	public void testCreateRealm() {
		String domainUid = "testCreateRealm.lan";
		getKeycloakAdminService().createRealm(domainUid);
		Realm realm = getKeycloakAdminService().getRealm(domainUid);
		assertNotNull(realm);
		assertEquals(domainUid, realm.id);
		assertTrue(realm.enabled);
		assertTrue(realm.internationalizationEnabled);
		assertTrue(realm.loginWithEmailAllowed);
		assertEquals(Duration.ofDays(30).toSeconds(), realm.accessCodeLifespanLogin);
	}

	@Test
	public void testDeleteRealm() {
		String domainUid = "testDeleteRealm.lan";
		getKeycloakAdminService().createRealm(domainUid);
		getKeycloakAdminService().deleteRealm(domainUid);
		Realm realm = getKeycloakAdminService().getRealm(domainUid);
		assertNull(realm);
	}

	@Test
	public void testDeleteUnknownRealm() {
		String domainUid = "testDeleteUnknownRealm.lan";
		try {
			getKeycloakAdminService().deleteRealm(domainUid);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testAllRealm() {
		getKeycloakAdminService().createRealm("testAllRealm1.lan");
		getKeycloakAdminService().createRealm("testAllRealm2.lan");
		List<Realm> all = getKeycloakAdminService().allRealms();
		assertEquals(3, all.size()); // 3 = testAllRealm1.lan, testAllRealm2.lan and keycloak master realm
	}

	@Test
	public void testFetchUnknownRealm() {
		assertNull(getKeycloakAdminService().getRealm("nope"));
	}

	@Test
	public void testOidcClient() {
		String domainUid = "testOidcClient.lan";
		String cli = IKeycloakUids.clientId(domainUid);
		getKeycloakAdminService().createRealm(domainUid);

		getKeycloakClientAdminService(domainUid).create(cli);

		OidcClient client = getKeycloakClientAdminService(domainUid).getOidcClient(cli);
		assertNotNull(client);

		String secret = getKeycloakClientAdminService(domainUid).getSecret(cli);
		assertEquals(client.secret, secret);
	}

	@Test
	public void testDeleteUnknownOidcClient() {
		String domainUid = "testDeleteUnknownOidcClient.lan";
		try {
			getKeycloakClientAdminService(domainUid).deleteOidcClient("nope");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testFetchUnknownOidcClient() {
		String domainUid = "testFetchUnknownOidcClient.lan";
		assertNull(getKeycloakClientAdminService(domainUid).getOidcClient("nope"));
	}

	@Test
	public void testFetchUnknownOidcClientSecret() {
		String domainUid = "testFetchUnknownOidcClientSecret.lan";
		assertNull(getKeycloakClientAdminService(domainUid).getSecret("nope"));
	}

	@Test
	public void testOidcClientUpdateRedirectUrls() throws Exception {
		String domainUid = "testOidcClientRedirectUrls.lan";
		String clientId = IKeycloakUids.clientId(domainUid);
		getKeycloakAdminService().createRealm(domainUid);

		getKeycloakClientAdminService(domainUid).create(clientId);

		OidcClient client = getKeycloakClientAdminService(domainUid).getOidcClient(clientId);
		assertEquals(1, client.redirectUris.size());
		assertEquals("https://configure_external_url_in_bluemind", client.redirectUris.iterator().next()); // the fake
																											// one

		String redirectUrl = "redirect.url";
		client.redirectUris = Set.of(redirectUrl);
		getKeycloakClientAdminService(domainUid).updateClient(clientId, client);

		client = getKeycloakClientAdminService(domainUid).getOidcClient(clientId);
		assertEquals(1, client.redirectUris.size());
		assertEquals(redirectUrl, client.redirectUris.iterator().next());
	}

	@Test
	public void testAuthFlow() {
		String domainUid = "testAuthFlow.lan";
		String flowAlias = "browser-bluemind";
		getKeycloakAdminService().createRealm(domainUid);

		getKeycloakFlowService(domainUid).createByCopying("browser", flowAlias);
		AuthenticationFlow authFlow = getKeycloakFlowService(domainUid).getAuthenticationFlow(flowAlias);
		assertNotNull(authFlow);
		assertEquals(flowAlias, authFlow.alias);

		getKeycloakFlowService(domainUid).deleteFlow(flowAlias);
		authFlow = getKeycloakFlowService(domainUid).getAuthenticationFlow(flowAlias);
		assertNull(authFlow);
	}

	@Test
	public void testFetchUnknownFlow() {
		String domainUid = "testFetchUnknownFlow.lan";
		assertNull(getKeycloakFlowService(domainUid).getAuthenticationFlow("nope"));
	}

	@Test
	public void testDeleteUnknownFlow() {
		String domainUid = "testDeleteUnknownFlow.lan";
		try {
			getKeycloakFlowService(domainUid).deleteFlow("nope");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testBlueMindProvider() {
		String domainUid = "testBlueMindProvider.lan";
		getKeycloakAdminService().createRealm(domainUid);

		String providerName = IKeycloakUids.bmProviderId(domainUid);
		BluemindProviderComponent bpComponent = new BluemindProviderComponent();
		bpComponent.parentId = domainUid;
		bpComponent.name = providerName;
		bpComponent.bmUrl = "http://localhost:8090";
		bpComponent.bmCoreToken = "toktok";

		getKeycloakBluemindProviderService(domainUid).create(bpComponent);

		BluemindProviderComponent provider = getKeycloakBluemindProviderService(domainUid)
				.getBluemindProvider(providerName);
		assertNotNull(provider);
		assertTrue(provider.enabled);
		assertEquals("http://localhost:8090", provider.bmUrl);

		getKeycloakBluemindProviderService(domainUid).deleteBluemindProvider(providerName);
		provider = getKeycloakBluemindProviderService(domainUid).getBluemindProvider(providerName);
		assertNull(provider);
	}

	@Test
	public void testKerberosProvider() {
		String domainUid = "testKerberosProvider.lan";
		getKeycloakAdminService().createRealm(domainUid);

		String providerName = IKeycloakUids.kerberosComponentName(domainUid);
		KerberosComponent krbComponent = new KerberosComponent();
		krbComponent.kerberosRealm = "TEST-DOMAIN.LOCAL";
		krbComponent.serverPrincipal = "HTTP/keycloak.test-domain.local@TEST-DOMAIN.LOCAL";
		krbComponent.keyTab = "/tmp/keytab";
		krbComponent.enabled = true;
		krbComponent.debug = true;
		krbComponent.cachePolicy = CachePolicy.DEFAULT;
		krbComponent.name = providerName;
		krbComponent.parentId = domainUid;

		getKeycloakKerberosService(domainUid).create(krbComponent);

		KerberosComponent provider = getKeycloakKerberosService(domainUid).getKerberosProvider(providerName);
		assertNotNull(provider);
		assertEquals(providerName, provider.name);
		assertEquals("HTTP/keycloak.test-domain.local@TEST-DOMAIN.LOCAL", provider.serverPrincipal);

		assertTrue(provider.enabled);

		getKeycloakKerberosService(domainUid).deleteKerberosProvider(providerName);
		provider = getKeycloakKerberosService(domainUid).getKerberosProvider(providerName);
		assertNull(provider);
	}

	@Test
	public void testFetchUnknownComponent() {
		String domainUid = "testFetchUnknownComponent.lan";
		assertNull(getKeycloakKerberosService(domainUid).getKerberosProvider("nope"));
		assertNull(getKeycloakBluemindProviderService(domainUid).getBluemindProvider("nope"));
	}

	@Test
	public void testDeleteUnknownComponent() {
		String domainUid = "testDeleteUnknownComponent.lan";
		try {
			getKeycloakKerberosService(domainUid).deleteKerberosProvider("nope");
		} catch (Exception e) {
			fail(e.getMessage());
		}

		try {
			getKeycloakBluemindProviderService(domainUid).deleteBluemindProvider("nope");
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}

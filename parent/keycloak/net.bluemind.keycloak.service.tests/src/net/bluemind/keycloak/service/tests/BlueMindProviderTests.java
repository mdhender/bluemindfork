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

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class BlueMindProviderTests extends AbstractServiceTests {

	@Override
	public void before() throws Exception {
		super.before();
		Topology.get().core().value.ip = getMyIpAddress();
	}

	@Test
	public void testBlueMindProvider() throws Exception {
		String domainUid = "bm.lan";
		String login = "toto";
		String password = "password";
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(login, password, domainUid, Routing.internal);

		HttpResponse<String> resp = login(domainUid, login, password);
		assertEquals(200, resp.statusCode());
		JsonObject result = new JsonObject(resp.body());
		String accessToken = result.getString("access_token");
		assertNotNull(accessToken);

		resp = login(domainUid, login, "wrongpassword");
		assertEquals(401, resp.statusCode());
	}

	private HttpResponse<String> login(String domainUid, String login, String password)
			throws URISyntaxException, IOException, InterruptedException {
		String endpoint = "http://" + new BmConfIni().get("keycloak") + ":8099/realms/" + domainUid
				+ "/protocol/openid-connect/token";
		Builder requestBuilder = HttpRequest.newBuilder(new URI(endpoint));
		requestBuilder.header("Charset", StandardCharsets.UTF_8.name());
		requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");
		String params = "grant_type=password";
		params += "&client_id=" + IKeycloakUids.clientId(domainUid);
		params += "&client_secret="
				+ getKeycloakClientAdminService(domainUid).getSecret(IKeycloakUids.clientId(domainUid));
		params += "&username=" + login + "@" + domainUid;
		params += "&password=" + password;
		byte[] postData = params.getBytes(StandardCharsets.UTF_8);
		requestBuilder.method("POST", HttpRequest.BodyPublishers.ofByteArray(postData));
		HttpRequest req = requestBuilder.build();
		HttpClient cli = HttpClient.newHttpClient();
		return cli.send(req, BodyHandlers.ofString());
	}

	private static String getMyIpAddress() {
		String ret = "127.0.0.1";
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						return tmp;
					}
				}
			}
		} catch (SocketException e) {
			// yeah yeah
		}
		return ret;
	}
}

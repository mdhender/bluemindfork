/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.authentication.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class NginxTests {
	private User user;
	private ItemValue<Domain> domain;
	private String[] userAlias;
	private String userLatd;
	private Server cyrus;

	@Before
	public void before() throws Exception {
		String domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		SystemConf currentValues = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues();
		currentValues.values.put(SysConfKeys.default_domain.name(), domainUid);
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
				.updateMutableValues(currentValues.values);

		cyrus = new Server();
		cyrus.ip = new BmConfIni().get("imap-role");
		cyrus.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(cyrus);

		domain = initDomain(domainUid, cyrus);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		Topology.get();
		System.err.println("---------------- setup ends ---------------");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void imapAuthenticationInvalidPassword() throws Exception {
		Response response = doNginxAuthenticationQuery("imap", "1143", userLatd, "invalidpassword");

		assertEquals(200, response.getStatusCode());
		assertEquals("Invalid login or password", response.getHeader("Auth-Status"));

		for (String alias : userAlias) {
			response = doNginxAuthenticationQuery("imap", "1143", alias, "invalidpassword");

			assertEquals(200, response.getStatusCode());
			assertEquals("Invalid login or password", response.getHeader("Auth-Status"));
		}
	}

	@Test
	public void imapAuthenticationInvalidLogin() throws Exception {
		Response response = doNginxAuthenticationQuery("imap", "1143", "invalidlogin@" + domain.uid, "password");

		assertEquals(200, response.getStatusCode());
		assertEquals("Invalid login or password", response.getHeader("Auth-Status"));
	}

	@Test
	public void imapLatdAuthenticationSuccess() throws Exception {
		Response response = doNginxAuthenticationQuery("imap", "1143", userLatd, "password");

		assertEquals(200, response.getStatusCode());
		assertEquals("OK", response.getHeader("Auth-Status"));
		assertEquals(cyrus.ip, response.getHeader("Auth-Server"));
		assertEquals("1143", response.getHeader("Auth-Port"));
		assertFalse(response.getHeaders().contains("Auth-User"));
	}

	@Test
	public void imapAliasAuthenticationSuccess() throws Exception {
		for (String alias : userAlias) {
			Response response = doNginxAuthenticationQuery("imap", "1143", alias, "password");

			assertEquals(200, response.getStatusCode());
			assertEquals("OK", response.getHeader("Auth-Status"));
			assertEquals(cyrus.ip, response.getHeader("Auth-Server"));
			assertEquals("1143", response.getHeader("Auth-Port"));
			assertTrue(response.getHeaders().contains("Auth-User"));
		}
	}

	@Test
	public void popLatdAuthenticationSuccess() throws Exception {
		Response response = doNginxAuthenticationQuery("pop3", "1110", userLatd, "password");

		assertEquals(200, response.getStatusCode());
		assertEquals("OK", response.getHeader("Auth-Status"));
		assertEquals(cyrus.ip, response.getHeader("Auth-Server"));
		assertEquals("1110", response.getHeader("Auth-Port"));
		assertFalse(response.getHeaders().contains("Auth-User"));
	}

	@Test
	public void popAliasAuthenticationSuccess() throws Exception {
		for (String alias : userAlias) {
			Response response = doNginxAuthenticationQuery("pop3", "1110", alias, "password");

			assertEquals(200, response.getStatusCode());
			assertEquals("OK", response.getHeader("Auth-Status"));
			assertEquals(cyrus.ip, response.getHeader("Auth-Server"));
			assertEquals("1110", response.getHeader("Auth-Port"));
			assertEquals(userLatd, response.getHeader("Auth-User"));
		}
	}

	@Test
	public void popAuthenticationInvalidPassword() throws Exception {
		Response response = doNginxAuthenticationQuery("pop3", "1110", userLatd, "invalidpassword");

		assertEquals(200, response.getStatusCode());
		assertEquals("Invalid login or password", response.getHeader("Auth-Status"));

		for (String alias : userAlias) {
			response = doNginxAuthenticationQuery("pop3", "1110", alias, "invalidpassword");

			assertEquals(200, response.getStatusCode());
			assertEquals("Invalid login or password", response.getHeader("Auth-Status"));
		}
	}

	@Test
	public void popAuthenticationInvalidLogin() throws Exception {
		Response response = doNginxAuthenticationQuery("pop3", "1110", "invalidlogin@" + domain.uid, "password");

		assertEquals(200, response.getStatusCode());
		assertEquals("Invalid login or password", response.getHeader("Auth-Status"));
	}

	@Test
	public void defaultDomainAuthentication() throws Exception {
		Response response = doNginxAuthenticationQuery("imap", "1143", user.login, "password");

		assertEquals(200, response.getStatusCode());
		assertEquals("OK", response.getHeader("Auth-Status"));
		assertEquals(cyrus.ip, response.getHeader("Auth-Server"));
		assertEquals("1143", response.getHeader("Auth-Port"));
		assertEquals(String.format("%s@%s", user.login, domain.value.name), response.getHeaders().get("Auth-User"));
	}

	private ItemValue<Domain> initDomain(String domainUid, Server... servers) throws Exception {
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);

		user = PopulateHelper.getUser("u1." + System.nanoTime(), domainUid, Routing.internal);
		user.password = "password";

		userLatd = user.login + "@" + domain.uid;
		userAlias = new String[] { user.login + ".alias@" + domainUid,
				user.login + "@" + domain.value.aliases.iterator().next() };

		List<Email> userEmails = new ArrayList<>();
		userEmails.addAll(user.emails);

		for (String alias : userAlias) {
			Email em = new Email();
			em.address = alias;
			em.isDefault = false;
			em.allAliases = false;
			userEmails.add(em);
		}
		user.emails = userEmails;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domainUid).create(user.login, user);

		return domain;
	}

	private AsyncHttpClient getHttpClient() {
		AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setConnectTimeout(120 * 1000) //
				.setReadTimeout(120 * 1000) //
				.setRequestTimeout(120 * 1000) //
				.setFollowRedirect(false) //
				.setMaxRedirects(0) //
				.setMaxRequestRetry(0) //
				.build();
		return new DefaultAsyncHttpClient(config);
	}

	private Response doNginxAuthenticationQuery(String protocol, String port, String login, String password)
			throws InterruptedException, ExecutionException, TimeoutException {
		RequestBuilder requestBuilder = new RequestBuilder();
		requestBuilder.setMethod("GET");
		requestBuilder.setUrl("http://localhost:8090/nginx");

		requestBuilder.addHeader("Client-IP", "10.0.0.34");
		requestBuilder.addHeader("Auth-Protocol", protocol);
		requestBuilder.addHeader("X-Auth-Port", port);
		requestBuilder.addHeader("Auth-User",
				login == null ? login : Base64.getEncoder().encodeToString(login.getBytes()));
		requestBuilder.addHeader("Auth-Pass",
				password == null ? password : Base64.getEncoder().encodeToString(password.getBytes()));

		AsyncHttpClient httpClient = getHttpClient();
		return httpClient.executeRequest(requestBuilder.build()).get(10, TimeUnit.SECONDS);
	}
}

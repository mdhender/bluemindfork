/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.xmpp.coresession.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class BaseXmppTests extends AsyncTestMethods {

	protected String domainName = "bm.lan";
	protected EventBus eventBus;
	private IUser userService;

	protected ItemValue<User> user1Item;
	protected ItemValue<User> user2Item;
	protected User user1;
	protected User user2;

	@Before
	public void before() throws Exception {
		JdbcActivator.getInstance().setDataSource(JdbcActivator.getInstance().getDataSource());

		// JdbcTestHelper.getInstance().beforeTest();
		// JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		// JdbcActivator.getInstance().setDataSource(
		// JdbcTestHelper.getInstance().getDataSource());
		//
		// final CountDownLatch cdl = new CountDownLatch(1);
		// Handler<AsyncResult<Void>> complete = new
		// Handler<AsyncResult<Void>>() {
		//
		// @Override
		// public void handle(AsyncResult<Void> event) {
		// cdl.countDown();
		// Assert.assertTrue(event.succeeded());
		//
		// }
		// };
		//
		// VertxPlatform.spawnVerticles(complete);
		// cdl.await();

		// FIXME start xmpp
		// TigaseStarter.start();

		eventBus = VertxPlatform.getVertx().eventBus();

		initBM();
	}

	@After
	public void after() throws Exception {
		// JdbcTestHelper.getInstance().afterTest();
	}

	private void initBM() throws Exception {
		// Server esServer = new Server();
		// esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		// esServer.tags = new String[] { "elasticsearch/event" };
		// PopulateHelper.initGlobalVirt(esServer);
		//
		// PopulateHelper.createTestDomain(domainName);

		userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainName);

		user1Item = defaultUser("test." + System.nanoTime());
		user1 = user1Item.value;
		create(user1Item);

		user2Item = defaultUser("test." + System.nanoTime());
		user2 = user2Item.value;
		create(user2Item);
	}

	private void create(ItemValue<User> user) throws ServerFault {
		userService.create(user.uid, user.value);
	}

	protected void initiateConnection(User user, final String sessionId) {
		initiateConnection(user, sessionId, true);
	}

	protected void initiateConnection(User user, final String sessionId, boolean handlePing) {

		Handler<Message<JsonObject>> sessionHandler = new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				eventBus.unregisterHandler("xmpp/session/" + sessionId, this);
				queueAssertValue("session", event.body());
			}
		};

		eventBus.registerHandler("xmpp/session/" + sessionId, sessionHandler);

		if (handlePing) {
			eventBus.registerHandler("xmpp/session/" + sessionId + "/ping", new Handler<Message<Void>>() {

				@Override
				public void handle(Message<Void> event) {
					event.reply();
				}
			});
		}
		eventBus.send("xmpp/sessions-manager:open",
				new JsonObject().putString("sessionId", sessionId).putString("latd", user.login + "@" + domainName),
				new Handler<Message<Void>>() {

					@Override
					public void handle(Message<Void> event) {
						queueAssertValue("conn", event.body());
					}
				});

		assertNull(waitAssert("conn"));

		JsonObject sessionObject = waitAssert("session");
		assertNotNull(sessionObject);

		eventBus.unregisterHandler("xmpp/session/" + sessionId, sessionHandler);
	}

	private ItemValue<User> defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainName;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		return ItemValue.create(UIDGenerator.uid(), user);
	}

	public String login(User user) {

		try {

			IAuthentication authService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IAuthentication.class);
			LoginResponse resp = authService.login(user.login + "@" + domainName, user.password, "xmpp-junit");

			System.err.println(resp.status);

			return resp.authKey;

		} catch (ServerFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}
}

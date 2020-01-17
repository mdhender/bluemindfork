package net.bluemind.xmpp.coresession.tests.ws;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Test;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.xmpp.coresession.tests.BaseXmppTests;

public class XmppWebsocketTests extends BaseXmppTests {

	@Test
	public void testWebSockConnect() throws Exception {

		AsyncHttpClient client = new DefaultAsyncHttpClient();
		String sessionId = login(user1);

		HttpVertxBus bus = new HttpVertxBus(client, "ws://localhost:8085/eventbus/websocket");

		bus.send(
				"xmpp/sessions-manager:open", new JsonObject().put("sessionId", sessionId)
						.put("latd", user1.login + "@" + domainName).put("password", "password"),
				new Handler<Message<JsonObject>>() {

					@Override
					public void handle(Message<JsonObject> event) {
						System.out.println("youyhhoooh");
					}
				});

		Thread.sleep(1000);
		bus.close();

		Thread.sleep(3000);
	}
}

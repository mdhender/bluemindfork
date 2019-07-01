package net.bluemind.xmpp.coresession.tests.ws;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.ning.http.client.AsyncHttpClient;

import net.bluemind.xmpp.coresession.tests.BaseXmppTests;

public class XmppWebsocketTests extends BaseXmppTests {

	@Test
	public void testWebSockConnect() throws Exception {

		AsyncHttpClient client = new AsyncHttpClient();
		String sessionId = login(user1);

		HttpVertxBus bus = new HttpVertxBus(client, "ws://localhost:8085/eventbus/websocket");

		bus.send(
				"xmpp/sessions-manager:open", new JsonObject().putString("sessionId", sessionId)
						.putString("latd", user1.login + "@" + domainName).putString("password", "password"),
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

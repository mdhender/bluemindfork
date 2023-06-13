package net.bluemind.imap.endpoint.notifications;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ImapIdleNotificationsDispatcher extends AbstractVerticle {

	public static class Reg implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new ImapIdleNotificationsDispatcher();
		}

	}

	@Override
	public void start() throws Exception {
		EventBus eb = vertx.eventBus();

		eb.consumer("mailreplica.mailbox.updated", (Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
			if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
				return;
			}

			JsonObject js = msg.body();

			JsonObject rebuilt = new JsonObject();
			rebuilt.put("containerUid", js.getString("container"));
			rebuilt.put("changes", js.getJsonArray("imapChanges"));

			eb.publish(Topic.IMAP_ITEM_NOTIFICATIONS, rebuilt);
		}, false));

		MQ.init(() -> {
			final Producer producer = MQ.registerProducer(Topic.IMAP_ITEM_NOTIFICATIONS);
			eb.consumer(Topic.IMAP_ITEM_NOTIFICATIONS, (Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
				if (StateContext.getState() != SystemState.CORE_STATE_RUNNING) {
					return;
				}
				producer.send(msg.body());
			}, false));

		});
	}

}

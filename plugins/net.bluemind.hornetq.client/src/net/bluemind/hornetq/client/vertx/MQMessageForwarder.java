package net.bluemind.hornetq.client.vertx;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MQMessageForwarder extends Verticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new MQMessageForwarder();
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(MQMessageForwarder.class);
	private List<IMessageForwarder> forwarders = null;

	public void start() {

		RunnableExtensionLoader<IMessageForwarder> loader = new RunnableExtensionLoader<IMessageForwarder>();
		forwarders = loader.loadExtensions("net.bluemind.hornetq", "forwardToVertx", "vertx-forwarder", "class");
		if (forwarders.isEmpty()) {
			return;
		}
		logger.info("start MQMessageForwared, forwarders : {}", forwarders.size());

		MQ.init(new IMQConnectHandler() {

			@Override
			public void connected() {

				for (IMessageForwarder forwarder : forwarders) {
					String topic = forwarder.getTopic();
					MQ.registerConsumer(topic, message -> {

						if (logger.isDebugEnabled()) {
							logger.debug(forwarder + " onMsg op:" + message);
						}

						forwarder.forward(getVertx(), message);

					});
				}
			}
		});
	}

}

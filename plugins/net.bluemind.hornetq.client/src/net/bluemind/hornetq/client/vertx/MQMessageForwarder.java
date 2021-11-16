package net.bluemind.hornetq.client.vertx;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.lib.vertx.IVerticleFactory;

public class MQMessageForwarder extends AbstractVerticle {

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

	@Override
	public void start() {
		RunnableExtensionLoader<IMessageForwarder> loader = new RunnableExtensionLoader<>();
		forwarders = loader.loadExtensions("net.bluemind.hornetq", "forwardToVertx", "vertx-forwarder", "class");
		if (forwarders.isEmpty()) {
			return;
		}
		logger.info("start MQMessageForwared, forwarders : {}", forwarders.size());

		MQ.init(() -> {
			for (IMessageForwarder forwarder : forwarders) {
				String topic = forwarder.getTopic();
				MQ.registerConsumer(topic, message -> {
					if (logger.isDebugEnabled()) {
						logger.debug("{} onMsg op: {}", forwarder, message);
					}
					forwarder.forward(getVertx(), message);
				});
			}
		});
	}

}

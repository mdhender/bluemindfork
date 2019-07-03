package net.bluemind.hornetq.client.vertx;

import org.vertx.java.core.Vertx;

import net.bluemind.hornetq.client.OOPMessage;

/**
 * This is used for forwarding a cluster message to local JVM event bus
 *
 */
public interface IMessageForwarder {

	String getTopic();

	void forward(Vertx vertx, OOPMessage message);

}

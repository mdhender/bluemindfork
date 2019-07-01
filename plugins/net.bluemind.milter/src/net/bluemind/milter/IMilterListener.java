package net.bluemind.milter;

import java.nio.ByteBuffer;

import org.apache.james.mime4j.dom.Message;

public interface IMilterListener {
	public static enum Status {
		CONTINUE, REJECT, DISCARD;
	}

	/**
	 * Called for each message
	 * 
	 * @param envelope
	 * @param message
	 */
	public Status onMessage(SmtpEnvelope envelope, Message message);

	/**
	 * Called to handle the envelope FROM command
	 * 
	 * @param from
	 * @return
	 */
	public Status onEnvFrom(String from);

	/**
	 * Called to handle the envelope RCPT command. One for each RCPT command
	 * 
	 * @param rcpt
	 * @return
	 */
	public Status onEnvRcpt(String rcpt);

	/**
	 * Called to handle message header
	 * 
	 * @param headerf
	 * @param headerv
	 * @return
	 */
	public Status onHeader(String headerf, String headerv);

	/**
	 * Called to handle the end of message headers
	 * 
	 * @return
	 */
	public Status onEoh();

	/**
	 * Called to handle a piece of a message's body
	 * 
	 * @param bodyp
	 * @return
	 */
	public Status onBody(ByteBuffer bodyp);
}

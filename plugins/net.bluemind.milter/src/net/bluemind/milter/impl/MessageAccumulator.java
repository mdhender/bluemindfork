package net.bluemind.milter.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.SmtpEnvelope;
import net.bluemind.mime4j.common.Mime4JHelper;

/**
 * Accumulate all events for ONE message to build {@link Message} and
 * {@link SmtpEnvelope}
 * 
 */
public class MessageAccumulator {
	private Logger logger = LoggerFactory.getLogger(MessageAccumulator.class);

	private FileBackedOutputStream current = new FileBackedOutputStream(32768, "message-accu");

	private SmtpEnvelope currentEnvelope = new SmtpEnvelope();
	private Message message;

	public MessageAccumulator() {
		logger.debug("new accumulator");
	}

	void connect(String hostname, InetAddress hostaddr, Properties properties) {

	}

	void envfrom(String[] argv, Properties properties) {
		if (logger.isDebugEnabled()) {
			for (String arg : argv) {
				logger.debug("from arg {}", arg);
			}

			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				logger.debug("prop {} {}", entry.getKey(), entry.getValue());
			}
		}

		currentEnvelope.setSender(new SmtpAddress(argv[0]));
	}

	void envrcpt(String[] argv, Properties properties) {
		currentEnvelope.addRecipient(new SmtpAddress(argv[0]));
	}

	void header(String headerf, String headerv) {
		logger.debug("header {} : {}", headerf, headerv);
		try {
			current.write(headerf.getBytes());
			current.write(": ".getBytes());
			current.write(headerv.getBytes());
			current.write("\n".getBytes());
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	void eoh() {

	}

	void body(ByteBuffer bodyp) {
		try {
			current.write("\n".getBytes());
			current.write(bodyp.array(), bodyp.arrayOffset(), bodyp.limit());
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	void done() {
		try {
			message = Mime4JHelper.parse(current.asByteSource().openStream());
		} catch (IOException e) {
			Throwables.propagate(e);
		} finally {
			try {
				current.reset();
			} catch (IOException e) {
			}
		}
		current = new FileBackedOutputStream(32768, "message-accu-done");
	}

	public SmtpEnvelope getEnvelope() {
		return currentEnvelope;
	}

	public Message getMessage() {
		return message;
	}

	void reset() {
		if (message != null) {
			message.dispose();
		}
		currentEnvelope = new SmtpEnvelope();
		try {
			current.reset();
		} catch (IOException e) {
		}
	}
}

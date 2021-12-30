package net.bluemind.milter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

	private Multimap<String, String> properties = ArrayListMultimap.create();
	private SmtpEnvelope currentEnvelope = new SmtpEnvelope();
	private Message message;

	private static final byte[] CRLF = "\r\n".getBytes();

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
		}

		storeProperties(properties);

		currentEnvelope.setSender(new SmtpAddress(argv[0]));
	}

	void envrcpt(String[] argv, Properties properties) {
		properties.put("rcpt", argv[0]);
		storeProperties(properties);
		currentEnvelope.addRecipient(new SmtpAddress(argv[0]));
	}

	void header(String headerf, String headerv) {
		logger.debug("header {} : {}", headerf, headerv);
		try {
			current.write(headerf.getBytes());
			current.write(": ".getBytes());
			current.write(headerv.getBytes());
			current.write(CRLF);
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	void eoh() {
		try {
			current.write(CRLF);
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	void body(ByteBuffer bodyp) {
		try {
			current.write(bodyp.array(), bodyp.arrayOffset(), bodyp.limit());
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	void done(Properties properties) {
		storeProperties(properties);

		try (InputStream in = current.asByteSource().openStream()) {
			message = Mime4JHelper.parse(in);
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

	public Map<String, Collection<String>> getProperties() {
		if (logger.isDebugEnabled()) {
			properties.keys().stream()
					.forEach(k -> logger.debug("Accumulator properties: {} {}", k, properties.get(k)));
		}

		return properties.asMap();
	}

	void reset() {
		if (message != null) {
			message.dispose();
		}
		currentEnvelope = new SmtpEnvelope();
		properties.clear();
		try {
			current.reset();
		} catch (IOException e) {
		}
	}

	public void helo(Properties properties) {
		storeProperties(properties);
	}

	private void storeProperties(Properties properties) {
		if (logger.isDebugEnabled()) {
			properties.entrySet()
					.forEach(entry -> logger.debug("Accumulator new property {} {}", entry.getKey(), entry.getValue()));
		}

		properties.keySet().stream().map(k -> k instanceof String ? (String) k : null).filter(Objects::nonNull)
				.forEach(k -> this.properties.put(k, properties.getProperty(k)));
	}
}

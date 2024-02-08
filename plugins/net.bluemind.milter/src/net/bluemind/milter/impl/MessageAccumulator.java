package net.bluemind.milter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import net.bluemind.jna.utils.MemfdSupport;
import net.bluemind.jna.utils.OffHeapTemporaryFile;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.SmtpEnvelope;
import net.bluemind.mime4j.common.Mime4JHelper;

/**
 * Accumulate all events for ONE message to build {@link Message} and
 * {@link SmtpEnvelope}
 * 
 */
public class MessageAccumulator {

	private static final Logger logger = LoggerFactory.getLogger(MessageAccumulator.class);

	private DataSink current;
	private Multimap<String, String> properties;
	private SmtpEnvelope currentEnvelope;
	private Message message;

	private static final byte[] CRLF = "\r\n".getBytes();

	public MessageAccumulator() {
		this.current = new DataSink();
		this.properties = ArrayListMultimap.create();
		this.currentEnvelope = new SmtpEnvelope();
	}

	void connect(String hostname, InetAddress hostaddr, Properties properties) {
		logger.debug("connect({}, {}, {})", hostname, hostaddr, properties);
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
			throw new AccumulatorException(e);
		}
	}

	void eoh() {
		try {
			current.write(CRLF);
		} catch (IOException e) {
			throw new AccumulatorException(e);
		}
	}

	void body(ByteBuffer bodyp) {
		try {
			current.write(bodyp.array(), bodyp.arrayOffset(), bodyp.limit());
		} catch (IOException e) {
			throw new AccumulatorException(e);
		}
	}

	void done(Properties properties) {
		storeProperties(properties);

		try (InputStream in = current.flipToReading()) {
			message = Mime4JHelper.parse(in);
		} catch (IOException e) {
			throw new AccumulatorException(e);
		} finally {
			current.reset();
		}
		current = new DataSink();
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
		current.reset();
	}

	public void helo(Properties properties) {
		storeProperties(properties);
	}

	private void storeProperties(Properties properties) {
		if (logger.isDebugEnabled()) {
			properties.entrySet()
					.forEach(entry -> logger.debug("Accumulator new property {} {}", entry.getKey(), entry.getValue()));
		}

		properties.keySet().stream().map(k -> k instanceof String casted ? casted : null).filter(Objects::nonNull)
				.forEach(k -> this.properties.put(k, properties.getProperty(k)));
	}

	private static class DataSink {
		private static final AtomicLong ALLOC = new AtomicLong();
		private final OffHeapTemporaryFile offHeap;
		private OutputStream output;

		public DataSink() {
			this.offHeap = MemfdSupport.newOffHeapTemporaryFile(fdName());
			try {
				this.output = offHeap.openForWriting();
			} catch (IOException e) {
				throw new AccumulatorException(e);
			}
		}

		public void write(byte[] bytes) throws IOException {
			output.write(bytes);
		}

		public void write(byte[] bytes, int off, int len) throws IOException {
			output.write(bytes, off, len);
		}

		public InputStream flipToReading() throws IOException {
			output.flush();
			output.close();
			return offHeap.openForReading();
		}

		private static final String fdName() {
			return "accum-" + ALLOC.incrementAndGet();
		}

		public void reset() {
			offHeap.close();
		}
	}

	@SuppressWarnings("serial")
	private static class AccumulatorException extends RuntimeException {
		public AccumulatorException(Throwable t) {
			super(t);
		}

	}
}

package net.bluemind.imap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.imap.sieve.SieveCodecFactory;
import net.bluemind.imap.sieve.SieveMessage;

public class SieveCodecFactoryTests {
	private SieveMessage message;

	@Before
	public void before() {
		this.message = null;
	}

	@Test
	public void testDecoderNormal() throws Exception {
		DummySession session = new DummySession();
		ProtocolDecoder decoder = new SieveCodecFactory().getDecoder(session);
		IoBuffer in = IoBuffer.wrap(("\"blabla\"\r\n" //
				+ "OK (ok) \"blabla\"\r\n").getBytes());
		decoder.decode(session, in, new ProtocolDecoderOutput() {

			@Override
			public void write(Object message) {
				SieveCodecFactoryTests.this.message = (SieveMessage) message;
			}

			@Override
			public void flush(NextFilter nextFilter, IoSession session) {

			}
		});

		Assert.assertNotNull(message);
	}

	@Test
	public void testDecoderBufferFlushInMessage() throws Exception {
		DummySession session = new DummySession();
		ProtocolDecoder decoder = new SieveCodecFactory().getDecoder(session);
		IoBuffer in = IoBuffer.wrap(("\"blabla\"\r\n").getBytes());
		decoder.decode(session, in, new ProtocolDecoderOutput() {

			@Override
			public void write(Object message) {
				SieveCodecFactoryTests.this.message = (SieveMessage) message;
			}

			@Override
			public void flush(NextFilter nextFilter, IoSession session) {

			}
		});
		Assert.assertNull(message);
		IoBuffer t = IoBuffer.wrap(("\"blabla\"\r\n" + "NO (error) \"blabla\"\r\n").getBytes());
		t.position(in.position());

		decoder.decode(session, t, new ProtocolDecoderOutput() {

			@Override
			public void write(Object message) {
				SieveCodecFactoryTests.this.message = (SieveMessage) message;
			}

			@Override
			public void flush(NextFilter nextFilter, IoSession session) {

			}
		});

		Assert.assertNotNull(message);
	}

	@Test
	public void testResponseWithLiteral() throws Exception {
		DummySession session = new DummySession();
		ProtocolDecoder decoder = new SieveCodecFactory().getDecoder(session);

		IoBuffer t = IoBuffer.wrap(
				("\"blabla\"\r\n" + "\"toto\" toto {10}\r\n" + "0123\n" + "45678\r\n" + "OK (error) \"blabla\"\r\n")
						.getBytes());

		decoder.decode(session, t, new ProtocolDecoderOutput() {

			@Override
			public void write(Object message) {
				SieveCodecFactoryTests.this.message = (SieveMessage) message;
			}

			@Override
			public void flush(NextFilter nextFilter, IoSession session) {

			}
		});

		Assert.assertNotNull(message);
		Assert.assertEquals(2, message.getLines().size());
		Assert.assertEquals("\"blabla\"", message.getLines().get(0));
		Assert.assertEquals("\"toto\" toto 0123\n45678", message.getLines().get(1));
		Assert.assertEquals("OK (error) \"blabla\"", message.getResponseMessage());
	}

	@Test
	public void testResponseWithLiteralAtTheEnd() throws Exception {
		DummySession session = new DummySession();
		ProtocolDecoder decoder = new SieveCodecFactory().getDecoder(session);

		IoBuffer t = IoBuffer.wrap(("OK (error) \"blabla\" {10}\r\n" //
				+ "0123\n" //
				+ "45678\r\n").getBytes());

		decoder.decode(session, t, new ProtocolDecoderOutput() {

			@Override
			public void write(Object message) {
				SieveCodecFactoryTests.this.message = (SieveMessage) message;
			}

			@Override
			public void flush(NextFilter nextFilter, IoSession session) {

			}
		});

		Assert.assertNotNull(message);
		Assert.assertEquals(0, message.getLines().size());

		Assert.assertEquals("OK (error) \"blabla\" 0123\n45678", message.getResponseMessage());
	}
}

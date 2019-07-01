package net.bluemind.unixsocket.tests;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import junit.framework.TestCase;
import net.bluemind.unixsocket.UnixClientSocket;
import net.bluemind.unixsocket.UnixDomainSocketChannel;

public class SaslTest extends TestCase {

	public void testSaslAuthValid() throws IOException {
		UnixClientSocket ucs = new UnixClientSocket("/var/run/saslauthd/mux");
		UnixDomainSocketChannel channel = ucs.connect();
		ByteBuffer bb = ByteBuffer.allocate(1024);
		addChunk(bb, "admin0");
		addChunk(bb, "admin");
		addChunk(bb, "imap");
		addChunk(bb, "");
		bb.flip();
		channel.write(bb);
		ByteBuffer read = ByteBuffer.allocate(1024);
		channel.read(read);
		read.flip();
		ByteBuf nettyBuf = Unpooled.wrappedBuffer(read);
		String readStuff = nettyBuf.toString(Charset.defaultCharset());
		assertTrue(readStuff.endsWith("OK"));
		channel.close();
		nettyBuf.release();
	}

	public void testSaslAuthInvalid() throws IOException {
		UnixClientSocket ucs = new UnixClientSocket("/var/run/saslauthd/mux");
		UnixDomainSocketChannel channel = ucs.connect();
		ByteBuffer bb = ByteBuffer.allocate(1024);
		addChunk(bb, "admin0");
		addChunk(bb, "wrong");
		addChunk(bb, "imap");
		addChunk(bb, "");
		bb.flip();
		channel.write(bb);
		ByteBuffer read = ByteBuffer.allocate(1024);
		channel.read(read);
		read.flip();
		ByteBuf nettyBuf = Unpooled.wrappedBuffer(read);
		String readStuff = nettyBuf.toString(Charset.defaultCharset());
		assertTrue(readStuff.endsWith("NO"));
		channel.close();
		nettyBuf.release();
	}

	private void addChunk(ByteBuffer bb, String s) {
		bb.put((byte) 0);
		bb.put((byte) s.length());
		bb.put(s.getBytes());
	}

}

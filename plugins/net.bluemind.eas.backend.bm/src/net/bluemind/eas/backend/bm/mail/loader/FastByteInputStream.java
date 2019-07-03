package net.bluemind.eas.backend.bm.mail.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteSource;

/**
 * Not synchronized version {@link ByteArrayInputStream}
 *
 */
public final class FastByteInputStream extends InputStream {

	private final byte buf[];

	private int pos;

	private int mark = 0;

	private final int count;

	public static ByteSource source(byte[] buf) {
		return new ByteSource() {

			@Override
			public InputStream openStream() throws IOException {
				return new FastByteInputStream(buf);
			}
		};
	}

	public FastByteInputStream(byte buf[]) {
		this.buf = buf;
		this.pos = 0;
		this.count = buf.length;
	}

	/**
	 * Creates <code>FastByteInputStream</code> that uses <code>buf</code> as
	 * its buffer array. The initial value of <code>pos</code> is
	 * <code>offset</code> and the initial value of <code>count</code> is the
	 * minimum of <code>offset+length</code> and <code>buf.length</code>. The
	 * buffer array is not copied. The buffer's mark is set to the specified
	 * offset.
	 *
	 * @param buf
	 *            the input buffer.
	 * @param offset
	 *            the offset in the buffer of the first byte to read.
	 * @param length
	 *            the maximum number of bytes to read from the buffer.
	 */
	public FastByteInputStream(byte buf[], int offset, int length) {
		this.buf = buf;
		this.pos = offset;
		this.count = Math.min(offset + length, buf.length);
		this.mark = offset;
	}

	public int read() {
		return (pos < count) ? (buf[pos++] & 0xff) : -1;
	}

	public int read(byte b[], int off, int len) {
		if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		}

		if (pos >= count) {
			return -1;
		}

		int avail = count - pos;
		if (len > avail) {
			len = avail;
		}
		if (len <= 0) {
			return 0;
		}
		System.arraycopy(buf, pos, b, off, len);
		pos += len;
		return len;
	}

	public long skip(long n) {
		long k = count - pos;
		if (n < k) {
			k = n < 0 ? 0 : n;
		}

		pos += k;
		return k;
	}

	public int available() {
		return count - pos;
	}

	public boolean markSupported() {
		return true;
	}

	public void mark(int readAheadLimit) {
		mark = pos;
	}

	public void reset() {
		pos = mark;
	}

	public void close() throws IOException {
	}

}

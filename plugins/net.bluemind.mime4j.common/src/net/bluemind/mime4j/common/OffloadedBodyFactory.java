/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mime4j.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.common.io.FileBackedOutputStream;

public class OffloadedBodyFactory implements BodyFactory {

	private static final Logger logger = LoggerFactory.getLogger(OffloadedBodyFactory.class);

	private static final String TMP_PREFIX = System.getProperty("net.bluemind.property.product", "unknown-jvm") + "-"
			+ OffloadedBodyFactory.class.getName();

	private static final class SizeStorage {

		private final FileBackedOutputStream fbos;
		private long size;

		public SizeStorage() {
			fbos = new FileBackedOutputStream(32768, TMP_PREFIX);
		}

		private void store(IStreamTransfer trans, InputStream in) throws IOException {
			size = trans.transfer(in, fbos);
		}

		public InputStream getInputStream() throws IOException {
			return fbos.asByteSource().openStream();
		}

		public int size() {
			return (int) size;
		}

		public void delete() {
			logger.debug("FBOS reset.");
			try {
				fbos.reset();
			} catch (IOException e) {
			}
		}

	}

	private static final Charset FALLBACK_CHARSET = CharsetUtil.DEFAULT_CHARSET;

	public interface SizedBody {
		int size();
	}

	public interface IStreamTransfer {

		long transfer(InputStream in, OutputStream out) throws IOException;
	}

	private final IStreamTransfer trans;

	/**
	 * All body parts transfers will use the same shared buffer.
	 * 
	 * If your {@link OffloadedBodyFactory} instance is not shared, it is faster to
	 * use that.
	 * 
	 * @return
	 */
	public static IStreamTransfer sharedBufferTransfer() {
		final byte[] forTransfers = new byte[8192];
		return (from, to) -> {
			long total = 0;
			while (true) {
				int r = from.read(forTransfers);
				if (r == -1) {
					break;
				}
				to.write(forTransfers, 0, r);
				total += r;
			}
			return total;
		};
	}

	public OffloadedBodyFactory() {
		this(ByteStreams::copy);
	}

	public OffloadedBodyFactory(IStreamTransfer trans) {
		this.trans = trans;
	}

	private SizeStorage store(InputStream in) throws IOException {
		SizeStorage ret = new SizeStorage();
		ret.store(trans, in);
		in.close();
		return ret;
	}

	private static class OffloadedBinary extends BinaryBody implements SizedBody {

		private final SizeStorage storage;

		public OffloadedBinary(SizeStorage storage) {
			this.storage = storage;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return storage.getInputStream();
		}

		@Override
		public int size() {
			return storage.size();
		}

		@Override
		public void dispose() {
			storage.delete();
		}

	}

	private static class OffloadedText extends TextBody implements SizedBody {

		private final SizeStorage storage;
		private final Charset charset;

		public OffloadedText(SizeStorage storage, Charset charset) {
			this.storage = storage;
			this.charset = charset;
		}

		@Override
		public int size() {
			return storage.size();
		}

		@Override
		public String getMimeCharset() {
			return charset.name();
		}

		@Override
		public Reader getReader() throws IOException {
			return new InputStreamReader(storage.getInputStream(), charset);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return storage.getInputStream();
		}

		@Override
		public void dispose() {
			storage.delete();
		}

	}

	/**
	 * Creates a {@link BinaryBody} that holds the content of the given input
	 * stream.
	 *
	 * @param is input stream to create a message body from.
	 * @return a binary body.
	 * @throws IOException if an I/O error occurs.
	 */
	public BinaryBody binaryBody(InputStream is) throws IOException {
		if (is == null)
			throw new IllegalArgumentException();

		SizeStorage storage = store(is);
		return new OffloadedBinary(storage);
	}

	/**
	 * Creates a {@link TextBody} that holds the content of the given input stream.
	 * <p>
	 * The charset corresponding to the given MIME charset name is used to decode
	 * the byte content of the input stream into a character stream when calling
	 * {@link TextBody#getReader() getReader()} on the returned object. If the MIME
	 * charset has no corresponding Java charset or the Java charset cannot be used
	 * for decoding then &quot;us-ascii&quot; is used instead.
	 *
	 * @param is          input stream to create a message body from.
	 * @param mimeCharset name of a MIME charset.
	 * @return a text body.
	 * @throws IOException if an I/O error occurs.
	 */
	public TextBody textBody(InputStream is, String mimeCharset) throws IOException {
		if (is == null)
			throw new IllegalArgumentException();
		if (mimeCharset == null)
			throw new IllegalArgumentException();

		SizeStorage storage = store(is);
		Charset charset = toJavaCharset(mimeCharset);
		return new OffloadedText(storage, charset);
	}

	private static Charset toJavaCharset(final String mimeCharset) {
		Charset charset = CharsetUtil.lookup(mimeCharset);
		if (charset == null) {
			logger.warn("MIME charset '{}' has no corresponding Java charset. Using {} instead.", mimeCharset,
					FALLBACK_CHARSET);
			return FALLBACK_CHARSET;
		}
		return charset;
	}

}

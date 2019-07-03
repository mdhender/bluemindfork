/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.mime4j.bodies;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.util.CharsetUtil;

import net.bluemind.mime4j.bodies.internal.DiscardBodyStorageProvider;
import net.bluemind.mime4j.bodies.internal.DiscardBodyStorageProvider.DiscardedStorage;

public class DiscardBodyFactory implements BodyFactory {

	private static final DiscardBodyStorageProvider storProv = new DiscardBodyStorageProvider();

	public interface IHasSize {
		public long size();
	}

	private static class SizedTextBody extends TextBody implements IHasSize {

		private final DiscardedStorage storage;
		private final Charset cs;
		private final String mimeCs;

		public SizedTextBody(DiscardedStorage storage, Charset charset, String mimeCharset) {
			this.storage = storage;
			this.cs = charset;
			this.mimeCs = mimeCharset;
		}

		@Override
		public String getMimeCharset() {
			return mimeCs;
		}

		@Override
		public Reader getReader() throws IOException {
			return new InputStreamReader(getInputStream(), cs);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return storage.getInputStream();
		}

		@Override
		public void dispose() {
			storage.delete();
		}

		@Override
		public long size() {
			return storage.size();
		}

	}

	private static class SizedBinaryBody extends BinaryBody implements IHasSize {

		private final DiscardedStorage storage;

		public SizedBinaryBody(DiscardedStorage storage) {
			this.storage = storage;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return storage.getInputStream();
		}

		@Override
		public void dispose() {
			storage.delete();
		}

		@Override
		public long size() {
			return storage.size();
		}

	}

	public DiscardBodyFactory() {
	}

	public BinaryBody binaryBody(InputStream is) throws IOException {
		if (is == null)
			throw new IllegalArgumentException();

		DiscardedStorage storage = storProv.store(is);
		return new SizedBinaryBody(storage);
	}

	public TextBody textBody(InputStream is, String mimeCharset) throws IOException {
		if (is == null)
			throw new IllegalArgumentException();
		if (mimeCharset == null)
			throw new IllegalArgumentException();

		DiscardedStorage storage = storProv.store(is);
		return new SizedTextBody(storage, toJavaCharset(mimeCharset), mimeCharset);
	}

	private static Charset toJavaCharset(final String mimeCharset) {
		Charset charset = CharsetUtil.lookup(mimeCharset);
		if (charset == null) {
			return StandardCharsets.US_ASCII;
		}
		return charset;
	}

}

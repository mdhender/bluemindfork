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
package net.bluemind.eas.backend.bm.mail.loader;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.Stream;
import net.bluemind.eas.data.formatter.HTMLBodyFormatter;
import net.bluemind.eas.data.formatter.PlainBodyFormatter;
import net.bluemind.eas.dto.base.AirSyncBaseRequest.BodyPreference;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Body;
import net.bluemind.eas.dto.base.AirSyncBaseResponse.NativeBodyType;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.BodyType;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.utils.CharsetUtils;

public class BodyAccumulator {

	private static final Logger logger = LoggerFactory.getLogger(BodyAccumulator.class);
	private final BodyType needed;
	private final int keepChars;
	private final TextAccumulator plain;
	private final TextAccumulator html;
	private FileBackedOutputStream fbos;

	private static class TextAccumulator {
		public final StringBuilder bodyContent = new StringBuilder();
		public boolean truncated = false;
		public int totalChars;
		public boolean foundSomething;
	}

	public BodyAccumulator(BodyOptions options) {
		if (options.bodyPrefs != null && !options.bodyPrefs.isEmpty()) {
			BodyPreference bp = options.bodyPrefs.get(0);
			needed = bp.type;
			if (bp.truncationSize != null) {
				keepChars = bp.truncationSize;
			} else {
				keepChars = Integer.MAX_VALUE;
			}
		} else {
			logger.debug("Defaulting to HTML");
			needed = BodyType.HTML;
			keepChars = Integer.MAX_VALUE;
		}
		plain = new TextAccumulator();
		html = new TextAccumulator();
	}

	public ByteBuf toByteBuf(Stream stream) throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Buffer> partContent = SyncStreamDownload.read(stream);
		Buffer partValue = partContent.get(15, TimeUnit.SECONDS);
		return partValue.getByteBuf();
	}

	public void consumeMime(Stream stream) {
		fbos = new FileBackedOutputStream(32768, "body-accu-consume");
		CompletableFuture<Void> partContent = SyncStreamDownload.read(stream, fbos);
		try {
			partContent.get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void consumeBodyPart(Part bodyPart, Stream stream) {
		TextAccumulator ta = null;
		if (Mime4JHelper.TEXT_HTML.equals(bodyPart.mime)) {
			ta = html;
		} else if (Mime4JHelper.TEXT_PLAIN.equals(bodyPart.mime)) {
			ta = plain;
		} else {
			logger.error("Unsupported mime {}", bodyPart.mime);
			return;
		}

		Charset charset = CharsetUtils.forName(bodyPart.charset);

		try {
			ByteBuf out = toByteBuf(stream);
			bodyAccumulate(ta, out.toString(charset));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void bodyAccumulate(TextAccumulator ta, String content) {
		ta.foundSomething = true;
		int storedChars = ta.bodyContent.length();
		int toAdd = content.length();
		ta.totalChars += toAdd;
		if (keepChars > 0) {
			if (storedChars < keepChars) {
				if (toAdd + storedChars > keepChars) {
					String toAppend = content.substring(0, keepChars - storedChars);
					ta.bodyContent.append(toAppend);
					ta.truncated = true;
				} else {
					ta.bodyContent.append(content);
				}
			}
		} else {
			ta.truncated = true;
			ta.bodyContent.delete(0, storedChars);
		}
	}

	public net.bluemind.eas.dto.base.AirSyncBaseResponse.Body body() {
		Body ret = new Body();
		ret.type = needed;
		if (needed == BodyType.MIME) {
			ret.data = DisposableByteSource.wrap(fbos);
		} else {
			switch (needed) {
			case HTML:
				if (html.foundSomething) {
					ret.data = DisposableByteSource.wrap(html.bodyContent.toString());
					ret.estimatedDataSize = html.totalChars;
					ret.truncated = html.truncated;
				} else if (plain.foundSomething) {
					if (plain.totalChars > 0) {
						ret.data = DisposableByteSource
								.wrap(new HTMLBodyFormatter().convert(plain.bodyContent.toString()));
					} else {
						ret.data = DisposableByteSource.wrap("");
					}
					ret.estimatedDataSize = plain.totalChars;
					ret.truncated = plain.truncated;
				} else {
					ret.data = DisposableByteSource.wrap("");
				}
				break;
			case PlainText:
				if (plain.foundSomething) {
					ret.data = DisposableByteSource.wrap(plain.bodyContent.toString());
					ret.estimatedDataSize = plain.totalChars;
					ret.truncated = plain.truncated;
				} else if (html.foundSomething) {
					String converted = new PlainBodyFormatter().convert(html.bodyContent.toString());
					ret.data = DisposableByteSource.wrap(converted);
					ret.estimatedDataSize = html.totalChars;
					ret.truncated = html.truncated;
				} else {
					ret.data = DisposableByteSource.wrap("...");
				}
				break;
			case RTF:
			case MIME:
			default:
				logger.error("Unreachable code path ?");
				break;

			}
		}
		return ret;
	}

	public NativeBodyType nativeBodyType() {
		switch (needed) {
		case PlainText:
			return NativeBodyType.PlainText;
		case RTF:
			return NativeBodyType.RTF;
		default:
		case MIME:
		case HTML:
			return NativeBodyType.HTML;
		}
	}

	public BodyType getBodyType() {
		return needed;
	}
}

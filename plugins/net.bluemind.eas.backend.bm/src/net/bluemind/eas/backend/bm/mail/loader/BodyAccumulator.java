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
	private final int truncationSize;
	private final TextAccumulator textAccumulator;

	private FileBackedOutputStream fbos;

	private static class TextAccumulator {
		public final StringBuilder bodyContent = new StringBuilder();
		public boolean truncated = false;
		public int contentLength;
		public BodyType bodyType;
	}

	public BodyAccumulator(BodyOptions options) {
		if (options.bodyPrefs != null && !options.bodyPrefs.isEmpty()) {
			BodyPreference bp = options.bodyPrefs.get(0);
			needed = bp.type;
			if (bp.truncationSize != null) {
				truncationSize = bp.truncationSize;
			} else {
				truncationSize = Integer.MAX_VALUE;
			}
		} else {
			needed = BodyType.HTML;
			truncationSize = Integer.MAX_VALUE;
		}
		textAccumulator = new TextAccumulator();
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
		if (Mime4JHelper.TEXT_HTML.equals(bodyPart.mime)) {
			textAccumulator.bodyType = BodyType.HTML;
		} else if (Mime4JHelper.TEXT_PLAIN.equals(bodyPart.mime)) {
			textAccumulator.bodyType = BodyType.PlainText;
		} else {
			logger.error("Unsupported mime {}", bodyPart.mime);
			return;
		}

		Charset charset = CharsetUtils.forName(bodyPart.charset);

		try {
			ByteBuf out = toByteBuf(stream);
			bodyAccumulate(out.toString(charset));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void bodyAccumulate(String content) {
		textAccumulator.contentLength = content.length();
		if (textAccumulator.contentLength > truncationSize) {
			String toAppend = content.substring(0, truncationSize);
			textAccumulator.bodyContent.append(toAppend);
			textAccumulator.truncated = true;
		} else {
			textAccumulator.bodyContent.append(content);
		}
	}

	public net.bluemind.eas.dto.base.AirSyncBaseResponse.Body body() {
		Body ret = new Body();
		ret.type = needed;

		if (needed == BodyType.RTF) {
			logger.error("Unsupported mime {}", needed);
			return ret;
		}

		if (needed == BodyType.MIME) {
			ret.data = DisposableByteSource.wrap(fbos);
			return ret;
		}

		if (textAccumulator.bodyType == needed) {
			ret.data = DisposableByteSource.wrap(textAccumulator.bodyContent.toString());
		} else if (needed == BodyType.HTML) {
			// device asks for HTML but we only have TEXT part
			ret.data = DisposableByteSource
					.wrap(new HTMLBodyFormatter().convert(textAccumulator.bodyContent.toString()));
		} else {
			// device asks for TEXT but we only have HTML part
			ret.data = DisposableByteSource
					.wrap(new PlainBodyFormatter().convert(textAccumulator.bodyContent.toString()));
		}

		ret.estimatedDataSize = textAccumulator.contentLength;
		ret.truncated = textAccumulator.truncated;

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

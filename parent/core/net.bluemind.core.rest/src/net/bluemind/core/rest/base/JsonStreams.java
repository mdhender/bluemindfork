/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.rest.base;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonEventType;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.core.streams.ReadStream;
import net.bluemind.lib.vertx.VertxPlatform;

public class JsonStreams {
	private static final Logger logger = LoggerFactory.getLogger(JsonStreams.class);

	private JsonStreams() {
	}

	public static CompletableFuture<Void> consume(ReadStream<Buffer> jsStream, Consumer<JsonObject> objectsHandler) {
		Objects.requireNonNull(jsStream, "read stream is null");
		Objects.requireNonNull(objectsHandler, "objects consumer is null");
		return VertxPlatform.getVertx().executeBlocking(prom -> streamJson(prom, jsStream, objectsHandler))
				.toCompletionStage().toCompletableFuture().thenApply(v -> null);

	}

	private static void streamJson(Promise<Object> pushed, ReadStream<Buffer> jsFile,
			Consumer<JsonObject> keyAndValueProc) {
		JsonParser parser = JsonParser.newParser().objectValueMode();
		jsFile.pause();
		parser.exceptionHandler(t -> {
			logger.error("parser error {}", t.getMessage());
			pushed.fail(t);
		});
		parser.endHandler(v -> pushed.tryComplete());
		parser.handler(js -> {
			if (js.type() == JsonEventType.VALUE) {
				keyAndValueProc.accept(js.objectValue());
			}
		});

		jsFile.exceptionHandler(t -> {
			logger.error("ERROR {}", t.getMessage(), t);
			pushed.tryFail(t);
		});
		jsFile.endHandler(v -> parser.end());
		jsFile.handler(parser::write);

		jsFile.resume();
	}

}

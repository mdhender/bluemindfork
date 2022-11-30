/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.utils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.JsonEventType;
import io.vertx.core.parsetools.JsonParser;
import io.vertx.core.streams.ReadStream;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.lib.vertx.VertxPlatform;

public class JsonStreams {

	private final CliContext ctx;

	public JsonStreams(CliContext ctx) {
		this.ctx = ctx;
	}

	public CompletableFuture<Void> consume(ReadStream<Buffer> jsStream, Consumer<JsonObject> objectsHandler) {
		Objects.requireNonNull(jsStream, "read stream is null");
		Objects.requireNonNull(objectsHandler, "objects consumer is null");
		Vertx vertx = VertxPlatform.getVertx();
		return vertx.executeBlocking(prom -> streamJson(prom, jsStream, objectsHandler)).toCompletionStage()
				.toCompletableFuture().thenApply(v -> null);

	}

	private void streamJson(Promise<Object> pushed, ReadStream<Buffer> jsFile, Consumer<JsonObject> keyAndValueProc) {
		JsonParser parser = JsonParser.newParser().objectValueMode();
		jsFile.pause();
		parser.exceptionHandler(t -> {
			ctx.error("parser error {}", t.getMessage());
			pushed.fail(t);
		});
		parser.endHandler(v -> pushed.tryComplete());
		parser.handler(js -> {
			if (js.type() == JsonEventType.VALUE) {
				keyAndValueProc.accept(js.objectValue());
			}
		});

		jsFile.exceptionHandler(t -> {
			ctx.error("ERROR {}", t.getMessage(), t);
			pushed.tryFail(t);
		});
		jsFile.endHandler(v -> parser.end());
		jsFile.handler(parser::write);

		jsFile.resume();
	}

}

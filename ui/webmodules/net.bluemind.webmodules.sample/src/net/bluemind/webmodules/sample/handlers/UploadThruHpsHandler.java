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
package net.bluemind.webmodules.sample.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.parsetools.RecordParser;

public class UploadThruHpsHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(UploadThruHpsHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		request.setExpectMultipart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {

			private int count = 0;
			private int imported = 0;

			@Override
			public void handle(HttpServerFileUpload upload) {

				upload.handler(RecordParser.newDelimited("END:VCARD", new Handler<Buffer>() {

					@Override
					public void handle(Buffer buff) {
						count++;
						importCard(buff.toString("utf-8"));

						imported++;

					}

				}));

				upload.endHandler(new Handler<Void>() {

					@Override
					public void handle(Void arg0) {

						String response = "{\"taskRefId\":1 , \"count\":" + count + " , \"imported\":" + imported
								+ ", \"complete\":true}";
						HttpServerResponse resp = request.response();
						resp.putHeader("Content-Length", "" + response.getBytes().length);

						resp.write(response);
						resp.setStatusCode(200);
						resp.end();

					}

				});
			}
		});
	}

	private void importCard(String string) {
		logger.info("receive {}", string);
	}

}

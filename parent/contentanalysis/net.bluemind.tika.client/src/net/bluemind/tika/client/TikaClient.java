/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.tika.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.content.analysis.ContentAnalyzer;

public class TikaClient implements ContentAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(TikaClient.class);
	private static final int MAX_TIMEOUT_SECS = 10;
	private static final String tikaUrl = "http://localhost:8087/tika";

	@Override
	public CompletableFuture<Optional<String>> extractText(InputStream in) {
		CompletableFuture<Optional<String>> ret = new CompletableFuture<>();
		String extracted = null;
		try {
			long time = System.currentTimeMillis();
			extracted = extract(in);
			time = System.currentTimeMillis() - time;
			logger.info("Tika parsed in {} ms.", time);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		ret.complete(Optional.ofNullable(extracted));
		return ret;
	}

	private String extract(InputStream in)
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		String ret;
		BoundRequestBuilder post = AHCHelper.get().preparePost(tikaUrl);
		post.setHeader("Content-Type", "binary/octet-stream");
		Response r = post.setBody(in).execute().get(MAX_TIMEOUT_SECS, TimeUnit.SECONDS);
		ret = r.getResponseBody();
		return ret;
	}

}

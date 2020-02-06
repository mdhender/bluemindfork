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
package net.bluemind.eas.http.tests.vertx;

import java.util.concurrent.CountDownLatch;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import net.bluemind.eas.testhelper.vertx.Wait;

public class TestResponseHandler implements Handler<HttpClientResponse> {

	private final CountDownLatch cdl;
	private int status;
	private MultiMap headers;

	public TestResponseHandler() {
		cdl = new CountDownLatch(1);
	}

	@Override
	public void handle(HttpClientResponse event) {
		status = event.statusCode();
		headers = event.headers();
		System.out.println("Got response: " + status + " " + event.statusMessage());
		event.bodyHandler(new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				System.out.println("request finished.");
				cdl.countDown();
			}
		});
	}

	public void waitForIt() {
		Wait.forIt(cdl);
	}

	public int status() {
		return status;
	}

	public MultiMap headers() {
		return headers;
	}

}

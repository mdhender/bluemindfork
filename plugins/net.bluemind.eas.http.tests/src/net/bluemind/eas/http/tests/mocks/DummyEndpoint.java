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
package net.bluemind.eas.http.tests.mocks;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;

public class DummyEndpoint implements IEasRequestEndpoint {

	public static boolean created;
	public static boolean handled;

	public DummyEndpoint() {
		created = true;
	}

	@Override
	public void handle(final AuthorizedDeviceQuery event) {
		handled = true;
		System.out.println("Handle " + event);
		event.request().endHandler(new Handler<Void>() {

			@Override
			public void handle(Void v) {
				HttpServerResponse resp = event.request().response();
				resp.headers().add("DummyHeader", "FakeValue");
				resp.setStatusCode(200).setStatusMessage("YEAH").end();
			}
		});
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("Dummy");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return true;
	}

}

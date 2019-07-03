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
package net.bluemind.webmodule.inputfilter;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import com.google.common.io.Resources;

import net.bluemind.webmodule.server.IWebFilter;

public class InputFilter implements IWebFilter {

	private static final Logger logger = LoggerFactory.getLogger(InputFilter.class);

	@Override
	public HttpServerRequest filter(HttpServerRequest request) {
		String path = request.path();
		if (path.startsWith("/input/")) {
			String b = path.substring("/input/".length());
			String bundleId = b.substring(0, b.indexOf("/"));
			String rp = b.substring(bundleId.length() + 1);

			logger.info("input !! {} : r {} !!!", b, rp);
			Bundle bundle = Platform.getBundle(bundleId);
			if (bundle != null) {
				URL url = bundle.getResource(rp);

				logger.info("resoruce {}", url);
				try {
					request.response().end(new Buffer(Resources.toByteArray(url)));
				} catch (IOException e) {

					request.response().setStatusCode(500).end();
					e.printStackTrace();
				}
			} else {
				request.response().setStatusCode(500).end();
			}
			return null;
		} else {
			return request;
		}
	}
}
